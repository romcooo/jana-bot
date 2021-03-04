package org.koppakurhiev.janabot.services

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.utils.ALogged

abstract class ABotService : ALogged(), IBotService {

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

    abstract class ACommand(override val trigger: String) : ALogged(), IBotService.ICommand
}
