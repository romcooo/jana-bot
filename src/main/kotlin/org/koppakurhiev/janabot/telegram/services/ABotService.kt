package org.koppakurhiev.janabot.telegram.services

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.ALogged
import org.koppakurhiev.janabot.telegram.bot.IBotService
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot

abstract class ABotService(override val bot: ITelegramBot) : ALogged(), IBotService {

    override fun help(message: Message?): String {
        val helpBuilder = StringBuilder()
        getCommands().forEach {
            if (it.help(message).isNotBlank()) {
                helpBuilder.append("\n")
                helpBuilder.appendLine(it.help(message))
            }
        }
        return helpBuilder.toString()
    }

    override suspend fun onMessage(message: Message) {
        //nothing
    }

    override suspend fun deconstruct() {
        //nothing
    }

    override suspend fun initialize() {
        //nothing
    }

    abstract class ABotCommand(override val service: IBotService, override val trigger: String) : ALogged(),
        IBotService.IBotCommand
}
