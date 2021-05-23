package org.koppakurhiev.janabot.telegram.services.backpack

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotSubCommand
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime

class CreateSubCommand(override val parent: BackpackService.BackpackCommand) : IBotSubCommand {
    override val command = "create"

    private val manager get() = parent.service.manager

    override fun getArguments(): Array<String> {
        return arrayOf("backpack name")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        if (arguments.isNullOrBlank()) {
            conversation.replyMessage(TelegramStrings.onMissingArgument(conversation.language, getArguments(), command))
            conversation.burnConversation(MessageLifetime.FLASH)
        } else if (arguments.contains(" ")) {
            conversation.replyMessage(BackpackStrings.getString(conversation.language, "create.space"))
            conversation.burnConversation(MessageLifetime.FLASH)
        } else {
            if (manager.createBackpack(message.chat.id, arguments)) {
                conversation.replyMessage(BackpackStrings.getString(conversation.language, "create", arguments))
            }
        }
    }

    override fun help(chat: Chat): String {
        return defaultHelp(BackpackStrings.getString(chat.getLocale(bot), "create.help"))
    }
}