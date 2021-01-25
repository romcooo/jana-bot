package org.koppakurhiev.janabot.commands

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.ACommand
import org.koppakurhiev.janabot.IBotService
import org.koppakurhiev.janabot.JanaBot

class Help(private val services: Array<IBotService>) : ACommand("/help") {
    override fun onCommand(message: Message, s: String?) {
        val messageBuilder = StringBuilder()
        messageBuilder.appendLine("\n Currently available commands are:")
            .appendLine("/help - get this message again")
            .appendLine("/start - say hello to your new bot")
        services.forEach { messageBuilder.append(it.help()) }
        JanaBot.bot.sendMessage(message.chat.id, messageBuilder.toString())
        logger.debug { "/help command executed in channel " + message.chat.id }
    }
}