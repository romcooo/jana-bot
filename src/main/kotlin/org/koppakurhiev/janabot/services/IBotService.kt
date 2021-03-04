package org.koppakurhiev.janabot.services

import com.elbekD.bot.types.Message

interface IBotService {
    fun help(message: Message?): String
    fun getCommands(): Array<ICommand>
    suspend fun onMessage(message: Message) {
        //nothing
    }

    interface ICommand {
        val trigger: String
        suspend fun onCommand(message: Message, s: String?)
        fun help(message: Message?): String
    }
}
