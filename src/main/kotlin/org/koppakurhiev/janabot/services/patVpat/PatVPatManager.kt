package org.koppakurhiev.janabot.services.patVpat

import com.elbekD.bot.types.Chat
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.services.patVpat.data.Answer
import org.koppakurhiev.janabot.services.patVpat.data.Question
import org.koppakurhiev.janabot.services.patVpat.data.Subscriber

class PatVPatManager {

    private val runningQuestion: Question? = null
    private val subscribers: List<Subscriber> = mutableListOf()
    private val questions: List<Question> = mutableListOf()
    private val answers: List<Answer> = mutableListOf()

    fun launch(): OperationResult {
        TODO()
    }

    private suspend fun changeQuestion(report: Boolean): OperationResult {
        TODO()
    }

    suspend fun askUser(chat: Chat): OperationResult {
        if (!isSubscribed(chat)) return OperationResult.NOT_SUBSCRIBED
        return if (runningQuestion != null) {
            val message = JanaBot.messages.get("5v5.ask", subscribers.size, runningQuestion.text)
            Conversation.startConversation(chat.id, message)
            OperationResult.SUCCESS
        } else {
            OperationResult.FAILURE
        }
    }

    suspend fun reportToUser(chat: Chat, numberOfAnswers: Int, question: String): OperationResult {
        if (!isSubscribed(chat)) return OperationResult.NOT_SUBSCRIBED
        Conversation.startConversation(chat.id, JanaBot.messages.get("5v5.fin", numberOfAnswers, question))
        return OperationResult.SUCCESS
    }

    fun subscribe(chat: Chat, username: String): OperationResult {
        TODO()
    }

    fun unsubscribe(chat: Chat): OperationResult {
        TODO()
    }

    fun remindersOn(chat: Chat): OperationResult {
        TODO()
    }

    fun remindersOff(chat: Chat): OperationResult {
        TODO()
    }

    fun addQuestion(text: String, creator: String): OperationResult {
        TODO()
    }

    fun answer(text: String, chat: Chat): OperationResult {
        TODO()
    }

    fun skipQuestion(): OperationResult {
        TODO()
    }

    fun isSubscribed(chat: Chat): Boolean {
        return subscribers.any { it.chatId == chat.id }
    }

    fun isQuestionAsked(): Boolean {
        return runningQuestion == null
    }

    private fun load(): OperationResult {
        TODO()
    }

    private fun saveQuestions(): OperationResult {
        TODO()
    }

    private fun saveSubscribers(): OperationResult {
        TODO()
    }

    private fun saveAnswers(): OperationResult {
        TODO()
    }

    enum class OperationResult {
        SUCCESS,
        SAVE_FAILED,
        LOAD_FAILED,
        NOT_VALID_CHAT,
        NOT_SUBSCRIBED,
        FAILURE
    }
}