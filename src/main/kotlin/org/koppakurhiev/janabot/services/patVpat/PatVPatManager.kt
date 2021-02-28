package org.koppakurhiev.janabot.services.patVpat

import com.elbekD.bot.types.Chat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.services.patVpat.data.*
import org.koppakurhiev.janabot.utils.ALogged
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.schedule

class PatVPatManager : ALogged() {

    private val questionTTL = Duration.ofMinutes(10)
    private val reminderTTL = Duration.ofMinutes(8)
    private val dataRepository = PatVPatRepository()
    private val questionsRepository = QuestionsRepository("Questions01")
    private var data = PatVPatData()
    private val timer = Timer()

    init {
        if (load() == OperationResult.LOAD_FAILED) {
            logger.error { "5v5 game loading failed!!" }
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
            val answers = data.answers.filter { it.questionId == oldQuestion.id }
            data.subscribers.forEach {
                reportToUser(it.chatId, answers.size, oldQuestion.text)
            }
        }
        data.runningQuestion = null
        val allQuestions = data.questions
        if (allQuestions == null) {
            logger.error { "No questions loaded from the DB" }
            return OperationResult.LOAD_FAILED
        }
        val unaskedQuestions = allQuestions.filter { !it.asked }
        if (unaskedQuestions.isEmpty()) {
            logger.info { "The game is out of questions" }
            data.subscribers.forEach {
                Conversation.startConversation(it.chatId, JanaBot.messages.get("5v5.outOfQuestions"))
            }
            setNextQuestion(Duration.ofDays(1))
            data.reminderAt = null
        } else {
            val newQuestion = unaskedQuestions.random()
            logger.info { "Asking new question: ${newQuestion.text}" }
            newQuestion.asked = true
            data.runningQuestion = newQuestion
            data.subscribers.forEach { askUser(it.chatId) }
            setNextQuestion(questionTTL)
            setReminder(reminderTTL)
        }
        val result = saveQuestions()
        if (result != OperationResult.SUCCESS) return OperationResult.SAVE_FAILED
        return saveData()
    }

    private fun setReminder(scheduleIn: Duration) {
        if (scheduleIn.isNegative) return
        val question = data.runningQuestion ?: return
        data.reminderAt = LocalDateTime.now().plus(scheduleIn)
        timer.schedule(scheduleIn.toMillis()) {
            GlobalScope.launch {
                if (question.id == data.runningQuestion?.id) {
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
                    if (question.id == data.runningQuestion?.id) {
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
        val toRemind = data.subscribers.filter { it.reminders && getAnswer(it.chatId, question.id) == null }
        logger.info { "Reminding users of ${question.text}" }
        toRemind.forEach {
            Conversation.startConversation(it.chatId, JanaBot.messages.get("5v5.reminder", question.text))
        }
    }

    suspend fun askUser(chatId: Long): OperationResult {
        if (!isSubscribed(chatId)) return OperationResult.NOT_SUBSCRIBED
        return if (isQuestionAsked()) {
            val message = JanaBot.messages.get(
                "5v5.ask",
                data.subscribers.size, data.runningQuestion!!.text
            )
            Conversation.startConversation(chatId, message)
            OperationResult.SUCCESS
        } else {
            OperationResult.NO_QUESTION_ASKED
        }
    }

    private fun getAnswer(chatId: Long, questionId: Long): String? {
        return data.answers.find { it.chatId == chatId && it.questionId == questionId }?.text
    }

    private suspend fun reportToUser(chatId: Long, numberOfAnswers: Int, question: String): OperationResult {
        if (!isSubscribed(chatId)) return OperationResult.NOT_SUBSCRIBED
        Conversation.startConversation(chatId, JanaBot.messages.get("5v5.fin", numberOfAnswers, question))
        return OperationResult.SUCCESS
    }

    fun subscribe(chat: Chat, username: String): OperationResult {
        logger.info { "$username has subscribed to the game" }
        if (chat.type != "private") return OperationResult.NOT_VALID_CHAT
        if (isSubscribed(chat.id)) return OperationResult.ALREADY_SUBSCRIBED
        val newSubscriber = Subscriber(chat.id, username, true)
        data.subscribers.add(newSubscriber)
        return saveData()
    }

    fun unsubscribe(chat: Chat): OperationResult {
        logger.info { "${chat.first_name} ${chat.last_name} has unsubscribed from the game" }
        if (!isSubscribed(chat.id)) return OperationResult.NOT_SUBSCRIBED
        data.subscribers.removeIf { it.chatId == chat.id }
        return saveData()
    }

    fun remindersOn(chat: Chat): OperationResult {
        if (!isSubscribed(chat.id)) return OperationResult.NOT_SUBSCRIBED
        val subscriberData = data.subscribers.find { it.chatId == chat.id }
        subscriberData?.reminders = true
        return saveData()
    }

    fun remindersOff(chat: Chat): OperationResult {
        if (!isSubscribed(chat.id)) return OperationResult.NOT_SUBSCRIBED
        val subscriberData = data.subscribers.find { it.chatId == chat.id }
        subscriberData?.reminders = false
        return saveData()
    }

    fun addQuestion(text: String, creator: String): OperationResult {
        val newQuestion = Question(generateNewQuestionId(), text, creator)
        logger.debug { "Recording question $text from $creator" }
        data.questions?.add(newQuestion) ?: return OperationResult.FAILURE
        return saveQuestions()
    }

    fun addAnswer(chat: Chat, text: String): OperationResult {
        logger.trace { "Recording an answer from ${chat.username}" }
        if (data.runningQuestion == null) return OperationResult.NO_QUESTION_ASKED
        val questionId = data.runningQuestion!!.id
        val oldAnswer = data.answers.find { it.chatId == chat.id && it.questionId == questionId }
        if (oldAnswer != null) {
            oldAnswer.text = text
        } else {
            val newAnswer = Answer(questionId, chat.id, text)
            data.answers.add(newAnswer)
        }
        return saveData()
    }

    suspend fun skipQuestion(chatId: Long): OperationResult {
        if (!isSubscribed(chatId)) return OperationResult.NOT_SUBSCRIBED
        if (data.runningQuestion == null) return OperationResult.NO_QUESTION_ASKED
        logger.info { "Skipping a question" }
        val skipped = data.runningQuestion!!
        data.subscribers.forEach {
            Conversation.startConversation(it.chatId, JanaBot.messages.get("5v5.skipNotice", skipped.text))
        }
        skipped.skipped = true
        return changeQuestion(false)
    }

    fun isSubscribed(chatId: Long): Boolean {
        return data.subscribers.any { it.chatId == chatId }
    }

    fun isQuestionAsked(): Boolean {
        return data.runningQuestion != null
    }

    fun getAskedQuestionsCount(): Int {
        val allQuestions = data.questions ?: return 0
        return allQuestions.filter { it.asked }.size
    }

    fun getSkippedQuestionsCount(): Int {
        val allQuestions = data.questions ?: return 0
        return allQuestions.filter { it.skipped }.size
    }

    fun getQuestionPoolSize(): Int {
        return data.questions?.size ?: 0
    }

    private fun generateNewQuestionId(): Long {
        return data.idCounter++
    }

    private fun load(): OperationResult {
        data = dataRepository.load() ?: return OperationResult.LOAD_FAILED
        data.questions = questionsRepository.load()?.toMutableList() ?: return OperationResult.LOAD_FAILED
        if (data.reminderAt != null) {
            setReminder(Duration.between(LocalDateTime.now(), data.reminderAt))
        }
        if (data.nextQuestionAt != null) {
            setNextQuestion(Duration.between(LocalDateTime.now(), data.nextQuestionAt))
        }
        return OperationResult.SUCCESS
    }

    private fun saveQuestions(): OperationResult {
        val questions = data.questions ?: return OperationResult.FAILURE
        return if (questionsRepository.save(questions))
            OperationResult.SUCCESS else OperationResult.SAVE_FAILED
    }

    private fun saveData(): OperationResult {
        return if (dataRepository.save(data))
            OperationResult.SUCCESS else OperationResult.SAVE_FAILED
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