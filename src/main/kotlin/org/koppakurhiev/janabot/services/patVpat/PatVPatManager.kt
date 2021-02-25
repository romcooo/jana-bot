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

    //TODO ADD LOGS!
    private val questionTTL = Duration.ofSeconds(60)
    private val reminderTTL = Duration.ofSeconds(45)
    private val dataRepository = PatVPatRepository()
    private val questionsRepository = QuestionsRepository()
    private val data = PatVPatData()
    private val timer = Timer()

    init {
        data.questions = mutableListOf()
    }

    suspend fun launch(): OperationResult {
        return changeQuestion(false)
    }

    private suspend fun changeQuestion(report: Boolean): OperationResult {
        val oldQuestion = data.runningQuestion
        if (oldQuestion != null && report) {
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
            data.subscribers.forEach {
                Conversation.startConversation(it.chatId, JanaBot.messages.get("5v5.outOfQuestions"))
            }
            setNextQuestion(Duration.ofDays(1))
        } else {
            val newQuestion = unaskedQuestions.random()
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
        val question = data.runningQuestion ?: return
        data.reminderAt = LocalDateTime.now().plus(scheduleIn)
        timer.schedule(scheduleIn.toMillis()) {
            GlobalScope.launch {
                if (question.id == data.runningQuestion?.id) {
                    data.reminderAt = null
                    fireReminder()
                }
            }
        }
    }

    private fun setNextQuestion(scheduleIn: Duration) {
        val question = data.runningQuestion ?: return
        data.nextQuestionAt = LocalDateTime.now().plus(scheduleIn)
        timer.schedule(scheduleIn.toMillis()) {
            GlobalScope.launch {
                if (question.id == data.runningQuestion?.id) {
                    data.nextQuestionAt = null
                    changeQuestion(true)
                }
            }
        }
    }

    private suspend fun fireReminder() {
        val question = data.runningQuestion?.text ?: return
        val toRemind = data.subscribers.filter { it.reminders }
        toRemind.forEach {
            Conversation.startConversation(it.chatId, JanaBot.messages.get("5v5.reminder", question))
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

    private suspend fun reportToUser(chatId: Long, numberOfAnswers: Int, question: String): OperationResult {
        if (!isSubscribed(chatId)) return OperationResult.NOT_SUBSCRIBED
        Conversation.startConversation(chatId, JanaBot.messages.get("5v5.fin", numberOfAnswers, question))
        return OperationResult.SUCCESS
    }

    fun subscribe(chat: Chat, username: String): OperationResult {
        if (chat.type != "private") return OperationResult.NOT_VALID_CHAT
        if (isSubscribed(chat.id)) return OperationResult.ALREADY_SUBSCRIBED
        val newSubscriber = Subscriber(chat.id, username, true)
        data.subscribers.add(newSubscriber)
        return saveData()
    }

    fun unsubscribe(chat: Chat): OperationResult {
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
        data.questions?.add(newQuestion) ?: return OperationResult.FAILURE
        return saveQuestions()
    }

    fun addAnswer(chat: Chat, text: String): OperationResult {
        if (data.runningQuestion == null) return OperationResult.NO_QUESTION_ASKED
        val questionId = data.runningQuestion!!.id
        val newAnswer = Answer(questionId, chat.id, text)
        data.answers.add(newAnswer)
        return saveData()
    }

    suspend fun skipQuestion(): OperationResult {
        if (data.runningQuestion == null) return OperationResult.NO_QUESTION_ASKED
        val skipped = data.runningQuestion!!
        changeQuestion(false)
        skipped.skipped = true
        val result = saveData()
        if (result != OperationResult.SUCCESS) return result
        return saveQuestions()
        //TODO print that question is skipped
    }

    fun isSubscribed(chatId: Long): Boolean {
        return data.subscribers.any { it.chatId == chatId }
    }

    fun isQuestionAsked(): Boolean {
        return data.runningQuestion != null
    }

    fun getQuestionPoolSize(): Int {
        return data.questions?.size ?: 0
    }

    private fun generateNewQuestionId(): Long {
        return data.idCounter++
    }

    private fun load(): OperationResult {
        TODO()
    }

    private fun saveQuestions(): OperationResult {
        return OperationResult.SUCCESS //TODO
    }

    private fun saveData(): OperationResult {
        return OperationResult.SUCCESS //TODO
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