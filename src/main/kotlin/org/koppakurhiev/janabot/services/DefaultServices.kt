package org.koppakurhiev.janabot.services

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot

class DefaultServices : ABotService() {

    private val commands: Array<IBotService.ICommand> = arrayOf(
        Help(),
        Start(),
    )

    override fun getCommands(): Array<IBotService.ICommand> {
        return commands
    }

    override fun help(): String {
        val helpBuilder = StringBuilder()
        commands.forEach {
            if (it.help().isNotBlank()) {
                helpBuilder.append("\n")
                helpBuilder.append(it.help())
            }
        }
        return helpBuilder.toString()
    }

    class Help : ACommand("/help") {
        override fun onCommand(message: Message, s: String?) {
            val messageBuilder = StringBuilder("Currently available commands are:")
            JanaBot.services.forEach {
                if (it.help().isNotBlank()) {
                    messageBuilder.appendLine(it.help())
                }
            }
            JanaBot.bot.sendMessage(message.chat.id, messageBuilder.toString())
            logger.debug { "/help command executed in channel " + message.chat.id }
        }

        override fun help(): String {
            return "/help - get this message again"
        }
    }

    class Start : ABotService.ACommand("/start") {
        override fun onCommand(message: Message, s: String?) {
            JanaBot.bot.sendMessage(
                message.chat.id,
                "Hi, ${message.from?.first_name ?: "person (it looks like you don't have a first_name)"}!"
            )
            logger.debug { "/start command executed in channel " + message.chat.id }
        }

        override fun help(): String {
            return "/start - say hello to your new bot"
        }
    }
}