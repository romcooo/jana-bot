package org.koppakurhiev.janabot.telegram.services.subgroups

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotCommand
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime

class TagCommand(override val service: SubGroupsService) : IBotCommand {
    override val command = "tag"

    private val subGroupsManager get() = service.subGroupsManager

    override fun getUiCommand(): BotCommand? {
        return null
    }

    override fun getArguments(): Array<String> {
        return arrayOf("groups")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(service.bot, message)
        val args = message.text?.split(" ")?.drop(1)
        if (args != null) {
            val text = tagMembers(subGroupsManager, conversation.chatId, *args.toTypedArray())
            val reply =
                conversation.replyMessage(
                    text ?: SubGroupStrings.getString(
                        conversation.language,
                        "noPeopleSelected",
                        args.toString()
                    )
                )
            conversation.burnMessage(reply, MessageLifetime.SHORT)
        } else {
            val reply =
                conversation.replyMessage(
                    TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                )
            conversation.burnMessage(reply, MessageLifetime.SHORT)
        }
    }

    override fun help(chat: Chat): String {
        return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "tag.help"))
    }

    override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
        return false
    }

    companion object {
        suspend fun tagMembers(subGroupsManager: SubGroupsManager, chatId: Long, vararg groupNames: String): String? {
            val users = HashSet<String>()
            groupNames.forEach {
                val members = subGroupsManager.getMembersList(it, chatId)
                if (members != null) {
                    users.addAll(members)
                }
            }
            if (users.isNotEmpty()) {
                val tags = StringBuilder()
                tags.append("Hey, ")
                users.forEach { user -> tags.append("@$user ") }
                return tags.toString()
            }
            return null
        }
    }

}
