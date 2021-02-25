package org.koppakurhiev.janabot.services.patVpat

import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.services.IBotService

class PatVPatService : ABotService() {

    private val patVPatManager = PatVPatManager()

    override fun getCommands(): Array<IBotService.ICommand> {
        return arrayOf(
            PatVPatCommand(patVPatManager)
        )
    }
}