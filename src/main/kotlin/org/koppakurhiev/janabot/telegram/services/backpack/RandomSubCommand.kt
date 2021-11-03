package org.koppakurhiev.janabot.telegram.services.backpack

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotSubCommand
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime

class RandomSubCommand(override val parent: BackpackService.BackpackCommand) : IBotSubCommand {
    override val command = "random"

    val manager get() = parent.service.manager

    override fun getArguments(): Array<String> {
        return arrayOf("Backpack name")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        if (arguments.isNullOrBlank()) {
            conversation.replyMessage(
                TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
            )
            conversation.burnConversation(MessageLifetime.FLASH)
        } else if (!manager.isBackpack(message.chat.id, arguments)) {
            conversation.replyMessage(BackpackStrings.getString(conversation.language, "notFound", arguments))
            conversation.burnConversation(MessageLifetime.FLASH)
        } else {
            val item = manager.popRandomFromBackpack(message.chat.id, arguments)
            if (item.isNullOrBlank()) {
                conversation.replyMessage(BackpackStrings.getString(conversation.language, "get.empty"))
                conversation.burnConversation(MessageLifetime.SHORT)
            } else {
                conversation.replyMessage(BackpackStrings.getString(conversation.language, "random", item))
            }
        }
    }

    override fun help(chat: Chat): String {
        return defaultHelp(BackpackStrings.getString(chat.getLocale(bot), "random.help"))
    }
}