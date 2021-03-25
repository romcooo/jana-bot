package org.koppakurhiev.janabot.telegram.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime
import org.koppakurhiev.janabot.telegram.services.ABotService

class TagCommand(service: SubGroupsService) : ABotService.ABotCommand(service, "/tag") {

    private val subGroupsManager = service.subGroupsManager

    override suspend fun onCommand(message: Message, s: String?) {
        val conversation = Conversation(service.bot, message)
        val args = message.text?.split(" ")?.drop(1)
        if (args != null) {
            val text = tagMembers(subGroupsManager, conversation.chatId, *args.toTypedArray())
            val reply =
                conversation.replyMessage(text ?: TelegramStrings.getString("tag.noPeopleSelected", args.toString()))
            conversation.burnMessage(reply, MessageLifetime.SHORT)
        } else {
            val reply = conversation.replyMessage(TelegramStrings.getString("tag.invalidFormat"))
            conversation.burnMessage(reply, MessageLifetime.SHORT)
        }
    }

    override fun help(message: Message?): String {
        return TelegramStrings.getString("tag.help")
    }

    companion object {
        fun tagMembers(subGroupsManager: SubGroupsManager, chatId: Long, vararg groupNames: String): String? {
            val users = HashSet<String>()
            groupNames.forEach {
                val currentGroup = subGroupsManager.getSubGroup(chatId, it)
                if (currentGroup != null) {
                    users.addAll(currentGroup.members)
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
