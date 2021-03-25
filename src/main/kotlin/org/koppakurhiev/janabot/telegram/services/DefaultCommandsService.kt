package org.koppakurhiev.janabot.telegram.services

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.CommonStrings
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotService
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime

class DefaultCommandsService(bot: ITelegramBot) : ABotService(bot) {

    private val commands: Array<IBotService.IBotCommand> = arrayOf(
        Help(this),
        Start(this),
        Invite(this),
    )

    override fun getCommands(): Array<IBotService.IBotCommand> {
        return commands
    }

    class Help(service: IBotService) : ABotCommand(service, "/help") {

        override fun getUiCommands(): List<BotCommand> {
            return listOf(BotCommand("help", "Prints the help text with all information."))
        }

        override suspend fun onCommand(message: Message, s: String?) {
            val conversation = Conversation(service.bot, message)
            val messageBuilder = StringBuilder(TelegramStrings.getString(key = "help.beginning"))
            service.bot.services.forEach {
                if (it.help(message).isNotBlank()) {
                    messageBuilder.append(it.help(message))
                }
            }
            conversation.replyMessage(messageBuilder.toString())
            conversation.burnConversation(MessageLifetime.MEDIUM)
            logger.debug { "/help command executed in channel " + message.chat.id }
        }

        override fun help(message: Message?): String {
            return TelegramStrings.getString(key = "help.help")
        }
    }

    class Start(service: IBotService) : ABotService.ABotCommand(service, "/start") {

        override fun getUiCommands(): List<BotCommand> {
            return listOf(BotCommand("start", "Gets the bot to talk to you."))
        }

        override suspend fun onCommand(message: Message, s: String?) {
            val conversation = Conversation(service.bot, message)
            val username = message.from?.username ?: CommonStrings.getString("person")
            conversation.replyMessage(TelegramStrings.getString("start.text", username))
            logger.debug { "/start command executed in channel " + message.chat.id }
        }

        override fun help(message: Message?): String {
            return TelegramStrings.getString("start.help")
        }
    }

    class Invite(service: IBotService) : ABotService.ABotCommand(service, "/invite") {

        override fun getUiCommands(): List<BotCommand> {
            return listOf(BotCommand("invite", "Gets you the link to the bots private chat."))
        }

        override suspend fun onCommand(message: Message, s: String?) {
            val conversation = Conversation(service.bot, message)
            conversation.replyMessage(
                TelegramStrings.getString("invite.text", service.bot.getName())
            )
        }

        override fun help(message: Message?): String {
            return TelegramStrings.getString("invite.help")
        }
    }
}
