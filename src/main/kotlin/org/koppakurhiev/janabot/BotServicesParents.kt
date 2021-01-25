package org.koppakurhiev.janabot

import com.elbekD.bot.types.Message
import mu.KotlinLogging

interface IBotService {
    fun help(): String
    fun getCommands(): Array<ICommand>
    suspend fun onMessage(message: Message) {
        //nothing
    }
}

interface ICommand {
    val trigger: String
    fun onCommand(message: Message, s: String?)
}

abstract class ABotService : IBotService {
    protected val logger = KotlinLogging.logger {}
}

abstract class ACommand(override val trigger: String) : ICommand {
    protected val logger = KotlinLogging.logger {}
}