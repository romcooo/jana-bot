package org.koppakurhiev.janabot.telegram.services.backpack

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotSubCommand
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime

class GetSubCommand(override val parent: BackpackService.BackpackCommand) : IBotSubCommand {
    override val command = "get"

    private val manager get() = parent.service.manager

    override fun getArguments(): Array<String> {
        return arrayOf("backpack name")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        if (arguments.isNullOrBlank()) {
            conversation.replyMessage(
                TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
            )
            conversation.burnConversation(MessageLifetime.FLASH)
        } else {
            val backpack = manager.getBackpack(conversation.chatId, arguments)
            when {
                backpack == null -> {
                    conversation.replyMessage(
                        BackpackStrings.getString(conversation.language, "notFound", arguments)
                    )
                }
                backpack.content.isEmpty() -> {
                    conversation.replyMessage(
                        BackpackStrings.getString(conversation.language, "get.empty")
                    )
                }
                else -> {
                    val backpackBuilder = StringBuilder()
                    backpack.content.forEach {
                        backpackBuilder.appendLine(it)
                    }
                    conversation.replyMessage(
                        BackpackStrings.getString(conversation.language, "get", backpackBuilder.toString())
                    )
                }
            }
        }
    }

    override fun help(chat: Chat): String {
        return defaultHelp(BackpackStrings.getString(chat.getLocale(bot), "get.help"))
    }
}