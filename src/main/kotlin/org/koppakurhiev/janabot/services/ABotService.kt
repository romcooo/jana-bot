package org.koppakurhiev.janabot.services

import org.koppakurhiev.janabot.utils.ALogged

abstract class ABotService : ALogged(), IBotService {

    override fun help(): String {
        val helpBuilder = StringBuilder()
        getCommands().forEach {
            if (it.help().isNotBlank()) {
                helpBuilder.append("\n")
                helpBuilder.appendLine(it.help())
            }
        }
        return helpBuilder.toString()
    }

    abstract class ACommand(override val trigger: String) : ALogged(), IBotService.ICommand
}
