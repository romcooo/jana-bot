package org.koppakurhiev.janabot.commands

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.ACommand
import org.koppakurhiev.janabot.JanaBot

class Start : ACommand("/start") {
    override fun onCommand(message: Message, s: String?) {
        JanaBot.bot.sendMessage(
            message.chat.id,
            "Hi, ${message.from?.first_name ?: "person (it looks like you don't have a first_name)"}!"
        )
        logger.debug { "/help command executed in channel " + message.chat.id }
    }
}