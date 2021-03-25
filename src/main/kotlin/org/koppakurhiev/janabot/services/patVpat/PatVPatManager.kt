package org.koppakurhiev.janabot.services.patVpat

import com.elbekD.bot.types.Chat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.persistence.MongoRepository
import org.koppakurhiev.janabot.services.patVpat.data.Answer
import org.koppakurhiev.janabot.services.patVpat.data.PatVPatData
import org.koppakurhiev.janabot.services.patVpat.data.Question
import org.koppakurhiev.janabot.services.patVpat.data.Subscriber
import org.koppakurhiev.janabot.utils.ALogged
import org.litote.kmongo.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.schedule

class PatVPatManager : ALogged() {

    private val questionTTL = Duration.ofDays(3)
    private val reminderTTL = Duration.ofDays(2)
    private val data: PatVPatData
    private val timer = Timer()
    private val answersCollection = MongoRepository.db.getCollection<Answer>("5v5_answers")
    private val questionsCollection = MongoRepository.db.getCollection<Question>("5v5_questions")

    init {
        val tmpData = MongoRepository.db.getCollection<PatVPatData>().findOne()
        if (tmpData == null) {
            data = PatVPatData()
            MongoRepository.db.getCollection<PatVPatData>().insertOne(data)
        } else {
            data = tmpData
        }
    }

    suspend fun launch(): OperationResult {
        logger.info { "Launching the 5v5 game!" }
        if (data.runningQuestion == null) {
            return changeQuestion(false)
        }
        return OperationResult.FAILURE
    }

    private suspend fun changeQuestion(report: Boolean): OperationResult {
        logger.trace { "Launching changeQuestion routine" }
        val oldQuestion = data.runningQuestion
        if (oldQuestion != null && report) {
            logger.debug { "Printing report to subscribed users" }
            val size = answersCollection.countDocuments(Answer::questionTag eq oldQuestion._id)
            broadcast(JanaBot.messages.get("5v5.fin", size, oldQuestion.text))
        }
        data.runningQuestion = null
        val unaskedQuestions = questionsCollection.find(Question::asked eq false).toList()
        if (unaskedQuestions.isEmpty()) {
            logger.info { "The game is out of questions" }
            broadcast(JanaBot.messages.get("5v5.outOfQuestions"))
            setNextQuestion(Duration.ofDays(1))
            data.reminderAt = null
        } else {
            val newQuestion = unaskedQuestions.random()
            logger.info { "Asking new question: ${newQuestion.text}" }
            newQuestion.asked = true
            if (!questionsCollection.updateOne(newQuestion).wasAcknowledged())
                return OperationResult.SAVE_FAILED
            data.runningQuestion = newQuestion
            val message = JanaBot.messages.get("5v5.ask", getSubscribersCount(), newQuestion.text)
            broadcast(message)
            setNextQuestion(questionTTL)
            setReminder(reminderTTL)
        }
        return updateData()
    }

    private fun updateData(): OperationResult {
        val collection = MongoRepository.db.getCollection<PatVPatData>()
        val saveResponse = collection.replaceOne(data)
        return if (saveResponse.wasAcknowledged()) OperationResult.SUCCESS else OperationResult.SAVE_FAILED
    }

    private fun setReminder(scheduleIn: Duration) {
        if (scheduleIn.isNegative) return
        val question = data.runningQuestion ?: return
        data.reminderAt = LocalDateTime.now().plus(scheduleIn)
        timer.schedule(scheduleIn.toMillis()) {
            GlobalScope.launch {
                if (question._id == data.runningQuestion?._id) {
                    data.reminderAt = null
                    fireReminder()
                } else {
                    logger.trace { "Leftover reminder expired" }
                }
            }
        }
        logger.debug { "Reminder set to ${data.reminderAt.toString()}" }
    }

    private fun setNextQuestion(scheduleIn: Duration) {
        if (scheduleIn.isNegative) return
        val question = data.runningQuestion
        if (question == null) {
            timer.schedule(scheduleIn.toMillis()) {
                GlobalScope.launch {
                    changeQuestion(false)
                }
            }
        } else {
            timer.schedule(scheduleIn.toMillis())
            {
                GlobalScope.launch {
                    if (question._id == data.runningQuestion?._id) {
                        data.nextQuestionAt = null
                        changeQuestion(true)
                    } else {
                        logger.trace { "Leftover timer expired" }
                    }
                }
            }
        }
        data.nextQuestionAt = LocalDateTime.now().plus(scheduleIn)
        logger.debug { "Question will expire at ${data.nextQuestionAt.toString()}" }
    }

    private suspend fun fireReminder() {
        val question = data.runningQuestion ?: return
        val toRemind = data.subscribers.filter { it.reminders && getAnswer(it.chatId, question._id) == null }
        logger.info { "Reminding users of ${question.text}" }
        toRemind.forEach {
            Conversation.startConversation(it.chatId, JanaBot.messages.get("5v5.reminder", question.text))
        }
    }

    suspend fun askUser(conversation: Conversation): OperationResult {
        if (!isSubscribed(conversation.chatId)) return OperationResult.NOT_SUBSCRIBED
        return if (isQuestionAsked()) {
            val message = JanaBot.messages.get(
                "5v5.ask",
                getSubscribersCount(), data.runningQuestion!!.text
            )
            conversation.replyMessage(message)
            OperationResult.SUCCESS
        } else {
            OperationResult.NO_QUESTION_ASKED
        }
    }

    suspend fun broadcast(messageText: String) {
        data.subscribers.forEach {
            Conversation.startConversation(it.chatId, messageText)
        }
    }

    private fun getAnswer(chatId: Long, questionId: Id<Question>): String? {
        return answersCollection.findOne(and(Answer::chatId eq chatId, Answer::questionTag eq questionId))?.text
    }

    fun subscribe(chat: Chat, username: String): OperationResult {
        logger.info { "$username has subscribed to the game" }
        if (chat.type != "private") return OperationResult.NOT_VALID_CHAT
        if (isSubscribed(chat.id)) return OperationResult.ALREADY_SUBSCRIBED
        val newSubscriber = Subscriber(chat.id, username, true)
        data.subscribers.add(newSubscriber)
        return updateData()
    }

    fun unsubscribe(chat: Chat): OperationResult {
        logger.info { "${chat.first_name} ${chat.last_name} has unsubscribed from the game" }
        if (chat.type != "private") return OperationResult.NOT_VALID_CHAT
        if (!isSubscribed(chat.id)) return OperationResult.NOT_SUBSCRIBED
        data.subscribers.removeIf { it.chatId == chat.id }
        return updateData()
    }

    fun remindersOn(chat: Chat): OperationResult {
        if (!isSubscribed(chat.id)) return OperationResult.NOT_SUBSCRIBED
        val subscriberData = data.subscribers.find { it.chatId == chat.id }
        subscriberData?.reminders = true
        return updateData()
    }

    fun remindersOff(chat: Chat): OperationResult {
        if (!isSubscribed(chat.id)) return OperationResult.NOT_SUBSCRIBED
        val subscriberData = data.subscribers.find { it.chatId == chat.id }
        subscriberData?.reminders = false
        return updateData()
    }

    fun addQuestion(text: String, creator: String): OperationResult {
        val newQuestion = Question(text = text, creator = creator)
        logger.debug { "Recording question $text from $creator" }
        if (!questionsCollection.insertOne(newQuestion).wasAcknowledged()) return OperationResult.SAVE_FAILED
        return OperationResult.SUCCESS
    }

    fun addAnswer(chat: Chat, text: String): OperationResult {
        logger.trace { "Recording an answer from ${chat.first_name} ${chat.last_name}" }
        if (chat.type != "private") return OperationResult.NOT_VALID_CHAT
        if (data.runningQuestion == null) return OperationResult.NO_QUESTION_ASKED
        if (!isSubscribed(chat.id)) return OperationResult.NOT_SUBSCRIBED
        val questionId = data.runningQuestion!!._id
        val oldAnswer = answersCollection.findOne(and(Answer::chatId eq chat.id, Answer::questionTag eq questionId))
        val success = if (oldAnswer != null) {
            oldAnswer.text = text
            answersCollection.updateOne(oldAnswer).wasAcknowledged()
        } else {
            val newAnswer = Answer(questionTag = questionId, chatId = chat.id, text = text)
            answersCollection.insertOne(newAnswer).wasAcknowledged()
        }
        return if (!success) OperationResult.SAVE_FAILED else OperationResult.SUCCESS
    }

    suspend fun skipQuestion(chat: Chat): OperationResult {
        if (chat.type != "private") return OperationResult.NOT_VALID_CHAT
        if (!isSubscribed(chat.id)) return OperationResult.NOT_SUBSCRIBED
        if (data.runningQuestion == null) return OperationResult.NO_QUESTION_ASKED
        logger.info { "Skipping a question" }
        val skipped = data.runningQuestion!!
        broadcast(JanaBot.messages.get("5v5.skipNotice", skipped.text))
        skipped.skipped = true
        questionsCollection.updateOne(skipped)
        return changeQuestion(false)
    }

    fun isSubscribed(chatId: Long): Boolean {
        return data.subscribers.any { it.chatId == chatId }
    }

    fun isQuestionAsked(): Boolean {
        return data.runningQuestion != null
    }

    fun getAskedQuestionsCount(): Int {
        return questionsCollection.countDocuments(Question::asked eq true).toInt()
    }

    fun getSkippedQuestionsCount(): Int {
        return questionsCollection.countDocuments(Question::skipped eq true).toInt()
    }

    fun getQuestionPoolSize(): Int {
        return questionsCollection.countDocuments().toInt()
    }

    fun getSubscribersCount(): Int {
        return data.subscribers.size
    }

    fun getCurrentQuestion(): String? {
        return data.runningQuestion?.text
    }

    enum class OperationResult {
        SUCCESS,
        SAVE_FAILED,
        LOAD_FAILED,
        NOT_VALID_CHAT,
        ALREADY_SUBSCRIBED,
        NOT_SUBSCRIBED,
        NO_QUESTION_ASKED,
        FAILURE
    }
}