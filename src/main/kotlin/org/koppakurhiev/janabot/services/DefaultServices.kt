package org.koppakurhiev.janabot.services

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.features.MessageLifetime

class DefaultServices : ABotService() {

    private val commands: Array<IBotService.ICommand> = arrayOf(
        Help(),
        Start(),
        Invite(),
    )

    override fun getCommands(): Array<IBotService.ICommand> {
        return commands
    }

    class Help : ACommand("/help") {

        override fun getUiCommands(): List<BotCommand> {
            return listOf(BotCommand("help", "Prints the help text with all information."))
        }

        override suspend fun onCommand(message: Message, s: String?) {
            val conversation = Conversation(message)
            val messageBuilder = StringBuilder(JanaBot.messages.get("help.beginning"))
            JanaBot.services.forEach {
                if (it.help(message).isNotBlank()) {
                    messageBuilder.append(it.help(message))
                }
            }
            conversation.replyMessage(messageBuilder.toString())
            conversation.burnConversation(MessageLifetime.MEDIUM)
            logger.debug { "/help command executed in channel " + message.chat.id }
        }

        override fun help(message: Message?): String {
            return JanaBot.messages.get("help.help")
        }
    }

    class Start : ABotService.ACommand("/start") {

        override fun getUiCommands(): List<BotCommand> {
            return listOf(BotCommand("start", "Gets the bot to talk to you."))
        }

        override suspend fun onCommand(message: Message, s: String?) {
            val conversation = Conversation(message)
            conversation.replyMessage(JanaBot.messages.get("start.text", message.from?.first_name ?: "person"))
            logger.debug { "/start command executed in channel " + message.chat.id }
        }

        override fun help(message: Message?): String {
            return JanaBot.messages.get("start.help")
        }
    }

    class Invite : ABotService.ACommand("/invite") {

        override fun getUiCommands(): List<BotCommand> {
            return listOf(BotCommand("invite", "Gets you the link to the bots private chat."))
        }

        override suspend fun onCommand(message: Message, s: String?) {
            val conversation = Conversation(message)
            conversation.replyMessage(
                JanaBot.messages.get(
                    "invite.text",
                    JanaBot.properties.getProperty("bot.username")
                )
            )
            conversation.burnConversation(MessageLifetime.MEDIUM)
        }

        override fun help(message: Message?): String {
            return JanaBot.messages.get("invite.help")
        }
    }
}
