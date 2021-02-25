package org.koppakurhiev.janabot.features

import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot

class Conversation(firstMessage: Message) {

    private val messageList = ArrayList<Message>()
    val chatId: Long

    init {
        messageList.add(firstMessage)
        chatId = firstMessage.chat.id
    }

    fun addMessage(message: Message) {
        messageList.add(message)
    }

    suspend fun sendMessage(text: String): Message {
        val message = JanaBot.bot.sendMessage(
            chatId,
            text
        ).await()
        messageList.add(message)
        return message
    }

    suspend fun replyMessage(text: String): Message {
        val message = JanaBot.bot.sendMessage(
            chatId = chatId,
            text = text,
            replyTo = messageList.last().message_id
        ).await()
        messageList.add(message)
        return message
    }

    fun burnConversation(timeToLive: MessageLifetime) {
        messageList.forEach { MessageCleaner.registerMessage(it, timeToLive) }
    }

    companion object {
        suspend fun startConversation(chatId: Long, text: String): Conversation {
            val message = JanaBot.bot.sendMessage(chatId, text).await()
            return Conversation(message)
        }
    }
}