package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.http.TelegramApiError
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.ALogged
import org.koppakurhiev.janabot.common.JobScheduler
import java.util.*
import kotlin.collections.ArrayList

class Conversation(val bot: ITelegramBot, firstMessage: Message) : ALogged() {

    private val messageList = ArrayList<Message>()
    private var willBurn = false
    val chatId: Long

    init {
        messageList.add(firstMessage)
        chatId = firstMessage.chat.id
    }

    fun addMessage(message: Message) {
        messageList.add(message)
    }

    suspend fun sendMessage(text: String): Message {
        val message = bot.getBot().sendMessage(
            chatId,
            text
        ).await()
        messageList.add(message)
        return message
    }

    suspend fun replyMessage(text: String): Message {
        val message = bot.getBot().sendMessage(
            chatId = chatId,
            text = text,
            replyTo = messageList.last().message_id
        ).await()
        messageList.add(message)
        return message
    }

    fun burnConversation(timeToLive: MessageLifetime) {
        if (!willBurn) {
            logger.trace { "Scheduling conversation to be deleted: $this in ${timeToLive.length / 1000} seconds." }
            JobScheduler.schedule(object : TimerTask() {
                override fun run() {
                    messageList.forEach {
                        try {
                            bot.getBot().deleteMessage(it.chat.id, it.message_id)
                        } catch (error: TelegramApiError) {
                            logger.warn("Telegram could not delete a message", error)
                        }
                    }
                }
            }, timeToLive.length)
            willBurn = true
        }

    }

    fun burnMessage(message: Message, timeToLive: MessageLifetime) {
        logger.trace { "Scheduling message to be deleted: $message in ${timeToLive.length / 1000} seconds." }
        JobScheduler.schedule(object : TimerTask() {
            override fun run() {
                try {
                    bot.getBot().deleteMessage(message.chat.id, message.message_id)
                } catch (error: TelegramApiError) {
                    logger.warn("Telegram could not delete a message", error)
                }
            }
        }, timeToLive.length)
    }

    companion object {
        suspend fun startConversation(bot: ITelegramBot, chatId: Long, text: String): Conversation {
            val message = bot.getBot().sendMessage(chatId, text).await()
            return Conversation(bot, message)
        }
    }
}

enum class MessageLifetime(val length: Long) {
    /**
     * 1 hour
     */
    LONG(3600000L),

    /**
     * 10 minutes
     */
    MEDIUM(600000L),

    /**
     * 2 minute
     */
    SHORT(120000L),

    /**
     * 30 seconds
     */
    FLASH(30000L)
}