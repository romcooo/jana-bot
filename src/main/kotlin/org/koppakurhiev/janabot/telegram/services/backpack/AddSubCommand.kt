package org.koppakurhiev.janabot.telegram.services.backpack

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.after
import org.koppakurhiev.janabot.common.getArg
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotSubCommand
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime

class AddSubCommand(override val parent: BackpackService.BackpackCommand) : IBotSubCommand {
    override val command = "add"

    private val manager get() = parent.service.manager

    override fun getArguments(): Array<String> {
        return arrayOf("backpack name", "value")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        val args = arguments?.split(" ")
        val backpackName = args?.getArg(0)
        val value = args?.after(0, " ")
        when {
            backpackName.isNullOrBlank() || value.isNullOrBlank() -> {
                conversation.replyMessage(
                    TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                )
                conversation.burnConversation(MessageLifetime.FLASH)
            }
            !manager.isBackpack(conversation.chatId, backpackName) -> {
                conversation.replyMessage(
                    BackpackStrings.getString(conversation.language, "notFound", backpackName)
                )
                conversation.burnConversation(MessageLifetime.SHORT)
            }
            manager.addToBackpack(conversation.chatId, backpackName, value) -> {
                conversation.replyMessage(
                    BackpackStrings.getString(conversation.language, "valueAdded", value)
                )
            }
            else -> {
                conversation.replyMessage(
                    BackpackStrings.getString(conversation.language, "duplicate", value)
                )
                conversation.burnConversation(MessageLifetime.SHORT)
            }
        }
    }

    override fun help(chat: Chat): String {
        return defaultHelp(BackpackStrings.getString(chat.getLocale(bot), "add.help"))
    }
}