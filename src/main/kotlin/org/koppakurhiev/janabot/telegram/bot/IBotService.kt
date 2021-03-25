package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.Message

interface IBotService {
    val bot: ITelegramBot
    fun help(message: Message?): String
    fun getCommands(): Array<IBotCommand>
    suspend fun initialize()
    suspend fun deconstruct()
    suspend fun onMessage(message: Message)

    interface IBotCommand {
        val service: IBotService
        val trigger: String
        suspend fun onCommand(message: Message, s: String?)
        fun help(message: Message?): String
        fun getUiCommands(): List<BotCommand> {
            return emptyList()
        }
    }
}
