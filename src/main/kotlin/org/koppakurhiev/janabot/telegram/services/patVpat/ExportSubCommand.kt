package org.koppakurhiev.janabot.telegram.services.patVpat

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message

class ExportSubCommand(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "export"

    override fun getArguments(): Array<String> {
        return emptyArray() //TODO
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        TODO("Not yet implemented")
    }

    override fun help(chat: Chat): String {
        return "" //TODO
    }
}