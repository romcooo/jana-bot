package org.koppakurhiev.janabot.telegram.services.backpack

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotSubCommand

class DeleteSubCommand(override val parent: BackpackService.BackpackCommand) : IBotSubCommand {
    override val command = "delete"

    val manager get() = parent.service.manager

    override fun getArguments(): Array<String> {
        return arrayOf("Backpack name")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        when {
            arguments.isNullOrBlank() -> {
                conversation.replyMessage(
                    TelegramStrings.onMissingArgument(
                        conversation.language,
                        getArguments(),
                        command
                    )
                )
            }
            manager.deleteBackpack(message.chat.id, arguments) -> {
                conversation.replyMessage(BackpackStrings.getString(conversation.language, "delete", arguments))
            }
            else -> {
                conversation.replyMessage(BackpackStrings.getString(conversation.language, "notFound", arguments))
            }
        }
    }

    override fun help(chat: Chat): String {
        return defaultHelp(BackpackStrings.getString(chat.getLocale(bot), "delete.help"))
    }
}