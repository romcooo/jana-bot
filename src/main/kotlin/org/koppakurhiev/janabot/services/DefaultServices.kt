package org.koppakurhiev.janabot.services

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.features.MessageLifetime

class DefaultServices : ABotService() {

    private val commands: Array<IBotService.ICommand> = arrayOf(
        Help(),
        Start(),
    )

    override fun getCommands(): Array<IBotService.ICommand> {
        return commands
    }

    class Help : ACommand("/help") {
        override suspend fun onCommand(message: Message, s: String?) {
            val conversation = Conversation(message)
            val messageBuilder = StringBuilder(JanaBot.messages.get("help.beginning"))
            JanaBot.getServices().forEach {
                if (it.help().isNotBlank()) {
                    messageBuilder.appendLine(it.help())
                }
            }
            conversation.sendMessage(messageBuilder.toString())
            conversation.burnConversation(MessageLifetime.SHORT)
            logger.debug { "/help command executed in channel " + message.chat.id }
        }

        override fun help(): String {
            return JanaBot.messages.get("help.help")
        }
    }

    class Start : ABotService.ACommand("/start") {
        override suspend fun onCommand(message: Message, s: String?) {
            val conversation = Conversation(message)
            conversation.sendMessage(JanaBot.messages.get("start.text", message.from?.first_name ?: "person"))
            logger.debug { "/start command executed in channel " + message.chat.id }
        }

        override fun help(): String {
            return JanaBot.messages.get("start.help")
        }
    }
}
