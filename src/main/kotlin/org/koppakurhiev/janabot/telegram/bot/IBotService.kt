package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message

interface IBotService {
    val bot: ITelegramBot
    val commands: Array<IBotCommand>

    suspend fun initialize() {
        commands.forEach {
            it.initialize()
        }
    }

    suspend fun deconstruct() {
        commands.forEach {
            it.deconstruct()
        }
    }

    fun help(chat: Chat): String {
        val helpBuilder = StringBuilder()
        commands.forEach {
            if (it.help(chat).isNotBlank()) {
                helpBuilder.append("\n")
                helpBuilder.append("/${it.help(chat)}")
            }
        }
        return helpBuilder.toString()
    }

    suspend fun onMessage(message: Message)
}
