package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.http.TelegramApiError
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.JobScheduler
import org.koppakurhiev.janabot.common.getLogger
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import java.util.*

class Conversation(val bot: ITelegramBot, val chatId: Long) {

    private val messageList = ArrayList<Message>()
    private val chatData: ChatData

    val language get() = chatData.language

    private var isOnFire = false
    var parseMode: ParseMode = ParseMode.DEFAULT

    constructor(bot: ITelegramBot, firstMessage: Message) : this(bot, firstMessage.chat.id) {
        messageList.add(firstMessage)
    }

    init {
        val chatDataCollection = bot.database.getCollection<ChatData>()
        var data = chatDataCollection.findOne(ChatData::chatId eq chatId)
        if (data == null) {
            data = ChatData(chatId = chatId)
            chatDataCollection.insertOne(data)
        }
        chatData = data
    }

    fun addMessage(message: Message) {
        messageList.add(message)
    }

    suspend fun sendMessage(text: String): Message {
        val message = bot.telegramBot.sendMessage(
            chatId,
            text,
            parseMode = parseMode.mode
        ).await()
        messageList.add(message)
        return message
    }

    suspend fun replyMessage(text: String): Message {
        val message = bot.telegramBot.sendMessage(
            chatId = chatId,
            text = text,
            replyTo = messageList.last().message_id,
            parseMode = parseMode.mode
        ).await()
        messageList.add(message)
        return message
    }

    fun burnConversation(timeToLive: MessageLifetime) {
        if (!isOnFire) {
            getLogger().trace { "Scheduling conversation to be deleted: $this in ${timeToLive.length / 1000} seconds." }
            JobScheduler.schedule(object : TimerTask() {
                override fun run() {
                    messageList.forEach {
                        try {
                            bot.telegramBot.deleteMessage(it.chat.id, it.message_id)
                        } catch (error: TelegramApiError) {
                            getLogger().warn("Telegram could not delete a message", error)
                        }
                    }
                }
            }, timeToLive.length)
            isOnFire = true
        }

    }

    fun burnMessage(message: Message, timeToLive: MessageLifetime) {
        getLogger().trace { "Scheduling message to be deleted: $message in ${timeToLive.length / 1000} seconds." }
        JobScheduler.schedule(object : TimerTask() {
            override fun run() {
                try {
                    bot.telegramBot.deleteMessage(message.chat.id, message.message_id)
                } catch (error: TelegramApiError) {
                    getLogger().warn("Telegram could not delete a message", error)
                }
            }
        }, timeToLive.length)
    }

    companion object {
        suspend fun startConversation(bot: ITelegramBot, chatId: Long, text: String): Conversation {
            val message = bot.telegramBot.sendMessage(chatId, text).await()
            return Conversation(bot, message)
        }
    }

    enum class ParseMode(val mode: String?) {
        MARKDOWN("Markdown"),
        HTML("HTML"),
        DEFAULT(null)
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