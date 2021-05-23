package org.koppakurhiev.janabot.telegram.services.backpack

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.getArg
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotSubCommand
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime

class RemoveSubCommand(override val parent: BackpackService.BackpackCommand) : IBotSubCommand {
    override val command = "remove"

    val manager get() = parent.service.manager

    override fun getArguments(): Array<String> {
        return arrayOf("Backpack name", "Item")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        val backpackName = arguments?.split(" ")?.getArg(0)
        val item = backpackName?.let { arguments.replaceFirst(it, "").trim() }
        if (backpackName.isNullOrBlank() || item.isNullOrBlank()) {
            conversation.replyMessage(TelegramStrings.onMissingArgument(conversation.language, getArguments(), command))
            conversation.burnConversation(MessageLifetime.FLASH)
        } else if (!manager.isBackpack(message.chat.id, backpackName)) {
            conversation.replyMessage(BackpackStrings.getString(conversation.language, "notFound", backpackName))
            conversation.burnConversation(MessageLifetime.FLASH)
        } else if (manager.removeFromBackpack(message.chat.id, backpackName, item)) {
            conversation.replyMessage(BackpackStrings.getString(conversation.language, "remove"))
        } else {
            conversation.replyMessage(BackpackStrings.getString(conversation.language, "remove.notFound"))
            conversation.burnConversation(MessageLifetime.SHORT)
        }
    }

    override fun help(chat: Chat): String {
        return defaultHelp(BackpackStrings.getString(chat.getLocale(bot), "remove.help"))
    }
}