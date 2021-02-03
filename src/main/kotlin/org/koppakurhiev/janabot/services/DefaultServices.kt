package org.koppakurhiev.janabot.services

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.LivingMessage
import org.koppakurhiev.janabot.sendMessage

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
            JanaBot.bot.sendMessage(message.chat.id, messageBuilder.toString(), replyTo = message.message_id, lifetime = LivingMessage.MessageLifetime.SHORT)
            // delete triggering message as well
            JanaBot.messageCleaner.registerMessage(
                LivingMessage(chatId = message.chat.id,
                    messageId = message.message_id,
                    lifetime = LivingMessage.MessageLifetime.SHORT)
            )
            logger.debug { "/help command executed in channel " + message.chat.id }
        }

        override fun help(): String {
            return "/help - get this message again"
        }
    }

    // This one doesn't clean up with JanaBot.messageCleaner on purpose, but feel free to change it
    class Start : ABotService.ACommand("/start") {
        override fun onCommand(message: Message, s: String?) {
            JanaBot.bot.sendMessage(
                message.chat.id,
                "Hi, ${message.from?.first_name ?: "person (it looks like you don't have a first_name)"}!",
                replyTo = message.message_id,
                lifetime = LivingMessage.MessageLifetime.FOREVER
            )
            logger.debug { "/start command executed in channel " + message.chat.id }
        }

        override fun help(): String {
            return "/start - say hello to your new bot"
        }
    }
}
