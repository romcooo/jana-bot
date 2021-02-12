package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.utils.SimpleConversationContext

class TagCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/tag") {
    private lateinit var conversationContext: SimpleConversationContext

    override suspend fun onCommand(message: Message, s: String?) {
        conversationContext = SimpleConversationContext(message.chat.id, message.message_id)
        var words = message.text?.split(" ")
        words = words?.subList(1, words.size)
        if (words != null) {
            val text = tagMembers(subGroupsManager, message.chat.id, *words.toTypedArray())
            conversationContext.sendMessage(
                text ?: "No people selected with given groups: $words",
                lifetime = if (text == null) MessageLifetime.MEDIUM
                else MessageLifetime.FOREVER
            )
        } else {
            conversationContext.sendMessage(
                "Invalid command format!\nUse /tag <group-name> ..",
                lifetime = MessageLifetime.SHORT
            )
        }
        // Do not delete original message!
    }

    companion object {
        fun tagMembers(
            subGroupsManager: SubGroupsManager,
            chatId: Long,
            vararg groupNames: String
        ): String? {
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
                for (user in users) {
                    tags.append("@$user ")
                }
                return tags.toString()
            }
            return null
        }
    }

}
