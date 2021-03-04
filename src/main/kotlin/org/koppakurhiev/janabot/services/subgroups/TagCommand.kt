package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.features.MessageCleaner
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.ABotService

class TagCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/tag") {

    override suspend fun onCommand(message: Message, s: String?) {
        val conversation = Conversation(message)
        var words = message.text?.split(" ")
        words = words?.subList(1, words.size)
        if (words != null) {
            val text = tagMembers(subGroupsManager, message.chat.id, *words.toTypedArray())
            MessageCleaner.registerMessage(
                conversation.replyMessage(
                    text ?: JanaBot.messages.get("tag.noPeopleSelected", words.toString())
                ), MessageLifetime.SHORT
            )
        } else {
            MessageCleaner.registerMessage(
                conversation.replyMessage(JanaBot.messages.get("tag.invalidFormat")),
                MessageLifetime.SHORT
            )
        }
    }

    override fun help(message: Message?): String {
        return JanaBot.messages.get("tag.help")
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
