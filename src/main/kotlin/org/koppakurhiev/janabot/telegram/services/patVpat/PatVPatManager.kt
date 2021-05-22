package org.koppakurhiev.janabot.telegram.services.patVpat

import com.elbekD.bot.http.await
import com.elbekD.bot.types.Chat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koppakurhiev.janabot.common.JobScheduler
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.common.getLogger
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.koppakurhiev.janabot.telegram.services.patVpat.data.Answer
import org.koppakurhiev.janabot.telegram.services.patVpat.data.PatVPatData
import org.koppakurhiev.janabot.telegram.services.patVpat.data.Question
import org.koppakurhiev.janabot.telegram.services.patVpat.data.Subscriber
import org.litote.kmongo.*
import java.time.Duration
import java.time.LocalDateTime

class PatVPatManager(val bot: ITelegramBot) {
    private val logger = getLogger()
    private val questionTTL = Duration.ofDays(2)
    private val reminderTTL = Duration.ofHours(37)
    private val data: PatVPatData
    private val answersCollection = bot.database.getCollection<Answer>("5v5_answers")
    private val questionsCollection = bot.database.getCollection<Question>("5v5_questions")

    init {
        val tmpData = bot.database.getCollection<PatVPatData>().findOne()
        if (tmpData == null) {
            data = PatVPatData()
            bot.database.getCollection<PatVPatData>().insertOne(data)
        } else {
            data = tmpData
        }
        if (data.reminderAt != null) {
            setReminder(Duration.between(LocalDateTime.now(), data.reminderAt))
        }
        if (data.nextQuestionAt != null) {
            setNextQuestion(Duration.between(LocalDateTime.now(), data.nextQuestionAt))
        }
    }

    suspend fun launch(): OperationResult {
        logger.info { "Launching the 5v5 game!" }
        return if (data.questionId != null) {
            logger.info { "Swapping the active question" }
            changeQuestion(true)
        } else {
            changeQuestion(false)
        }
    }

    private suspend fun changeQuestion(report: Boolean): OperationResult {
        logger.trace { "Launching changeQuestion routine" }
        val oldQuestion = data.questionId?.let { questionsCollection.findOneById(it) }
        if (oldQuestion != null && report) {
            logger.debug { "Printing report to subscribed users" }
            val size = answersCollection.countDocuments(Answer::questionTag eq oldQuestion._id)
            broadcast { chat -> PatVPatStrings.getString(chat.getLocale(bot), "finish", size, oldQuestion.text) }
        }
        data.questionId = null
        val unaskedQuestions = questionsCollection.find(Question::asked eq false).toList()
        if (unaskedQuestions.isEmpty()) {
            logger.info { "The game is out of questions" }
            broadcast { chat -> PatVPatStrings.getString(chat.getLocale(bot), "outOfQuestions") }
            setNextQuestion(Duration.ofDays(1))
            data.reminderAt = null
        } else {
            val newQuestion = unaskedQuestions.random()
            logger.info { "Asking new question: ${newQuestion.text}" }
            newQuestion.asked = true
            if (!questionsCollection.updateOne(newQuestion).wasAcknowledged())
                return OperationResult.SAVE_FAILED
            data.questionId = newQuestion._id
            broadcast { chat ->
                PatVPatStrings.getString(
                    chat.getLocale(bot),
                    "questionStatement",
                    getSubscribersCount(),
                    newQuestion.text
                )
            }
            setNextQuestion(questionTTL)
            setReminder(reminderTTL)
        }
        return updateData()
    }

    private fun updateData(): OperationResult {
        val collection = bot.database.getCollection<PatVPatData>()
        val saveResponse = collection.replaceOne(data)
        return if (saveResponse.wasAcknowledged()) OperationResult.SUCCESS else OperationResult.SAVE_FAILED
    }

    private fun setReminder(scheduleIn: Duration) {
        if (scheduleIn.isNegative) return
        val question = data.questionId ?: return
        data.reminderAt = LocalDateTime.now().plus(scheduleIn)
        JobScheduler.schedule(scheduleIn.toMillis()) {
            GlobalScope.launch {
                if (question == data.questionId) {
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
        val question = data.questionId
        if (question == null) {
            JobScheduler.schedule(scheduleIn.toMillis()) {
                GlobalScope.launch {
                    changeQuestion(false)
                }
            }
        } else {
            JobScheduler.schedule(scheduleIn.toMillis())
            {
                GlobalScope.launch {
                    if (question == data.questionId) {
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
        val questionId = data.questionId ?: return
        val toRemind = data.subscribers.filter { it.reminders && getAnswer(it.chatId, questionId) == null }
        val text = getCurrentQuestion()
        logger.info { "Reminding users of $text" }
        toRemind.forEach {
            val conversation = Conversation(bot, it.chatId)
            conversation.sendMessage(PatVPatStrings.getString(conversation.language, "reminder", text))
        }
    }

    suspend fun askUser(conversation: Conversation): OperationResult {
        if (!isSubscribed(conversation.chatId)) return OperationResult.NOT_SUBSCRIBED
        return if (isQuestionAsked()) {
            val message = PatVPatStrings.getString(
                conversation.language,
                "questionStatement",
                getSubscribersCount(),
                getCurrentQuestion()
            )
            conversation.replyMessage(message)
            OperationResult.SUCCESS
        } else {
            OperationResult.NO_QUESTION_ASKED
        }
    }

    suspend fun broadcast(buildMessage: (chat: Chat) -> String) {
        logger.trace { "Broadcasting a message" }
        data.subscribers.forEach {
            val chat = bot.telegramBot.getChat(it.chatId).await()
            val text = buildMessage.invoke(chat)
            Conversation.startConversation(bot, chat.id, text)
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
        logger.debug { "Turning on reminders for ${chat.id}" }
        return updateData()
    }

    fun remindersOff(chat: Chat): OperationResult {
        if (!isSubscribed(chat.id)) return OperationResult.NOT_SUBSCRIBED
        val subscriberData = data.subscribers.find { it.chatId == chat.id }
        subscriberData?.reminders = false
        logger.debug { "Turning of reminders for ${chat.id}" }
        return updateData()
    }

    fun addQuestion(text: String, creator: String): OperationResult {
        val newQuestion = Question(text = text, creator = creator)
        logger.info { "Recording question $text from $creator" }
        if (!questionsCollection.insertOne(newQuestion).wasAcknowledged()) return OperationResult.SAVE_FAILED
        return OperationResult.SUCCESS
    }

    fun addAnswer(chat: Chat, text: String): OperationResult {
        logger.info { "Recording an answer from ${chat.first_name} ${chat.last_name}" }
        if (chat.type != "private") return OperationResult.NOT_VALID_CHAT
        if (data.questionId == null) return OperationResult.NO_QUESTION_ASKED
        if (!isSubscribed(chat.id)) return OperationResult.NOT_SUBSCRIBED
        val oldAnswer =
            answersCollection.findOne(and(Answer::chatId eq chat.id, Answer::questionTag eq data.questionId))
        val success = if (oldAnswer != null) {
            oldAnswer.text = text
            answersCollection.updateOne(oldAnswer).wasAcknowledged()
        } else {
            val newAnswer = Answer(questionTag = data.questionId, chatId = chat.id, text = text)
            answersCollection.insertOne(newAnswer).wasAcknowledged()
        }
        return if (!success) OperationResult.SAVE_FAILED else OperationResult.SUCCESS
    }

    suspend fun skipQuestion(initChat: Chat): OperationResult {
        if (initChat.type != "private") return OperationResult.NOT_VALID_CHAT
        if (!isSubscribed(initChat.id)) return OperationResult.NOT_SUBSCRIBED
        if (data.questionId == null) return OperationResult.NO_QUESTION_ASKED
        val skipped = questionsCollection.findOneById(data.questionId!!) ?: return OperationResult.FAILURE
        logger.info { "Skipping the question '${skipped.text}'" }
        broadcast { chat -> PatVPatStrings.getString(chat.getLocale(bot), "skipNotice", skipped.text) }
        skipped.skipped = true
        questionsCollection.updateOne(skipped)
        return changeQuestion(false)
    }

    fun isSubscribed(chatId: Long): Boolean {
        return data.subscribers.any { it.chatId == chatId }
    }

    fun isQuestionAsked(): Boolean {
        return data.questionId != null
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
        return data.questionId?.let { questionsCollection.findOneById(it)?.text }
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