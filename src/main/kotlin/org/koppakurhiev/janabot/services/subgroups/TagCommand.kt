package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.SimpleConversationContext
import org.koppakurhiev.janabot.features.LivingMessage
import org.koppakurhiev.janabot.services.ABotService

class TagCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/tag") {
    private lateinit var conversationContext: SimpleConversationContext

    override fun onCommand(message: Message, s: String?) {
        conversationContext = SimpleConversationContext(message.chat.id, message.message_id)
        var words = message.text?.split(" ")
        words = words?.subList(1, words.size)
        if (words != null) {
            val text = tagMembers(subGroupsManager, message.from?.username, message.chat.id, *words.toTypedArray())
            // TODO currently the message below will be FOREVER even if text is null
            conversationContext.sendMessage(text ?: "No people selected with given groups: $words", lifetime = LivingMessage.MessageLifetime.FOREVER)
        } else {
            conversationContext.sendMessage("Invalid command format!\n" +
                        "Use /tag <group-name> .." ,
                lifetime = LivingMessage.MessageLifetime.SHORT
            )
        }
        // Do not delete original message!
    }

    companion object {
        fun tagMembers(subGroupsManager: SubGroupsManager, username: String?, chatId: Long, vararg groupNames: String): String? {
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
                // removed in I#9 - https://github.com/romcooo/jana-bot/issues/9
//                tags.append("- ${username ?: "A person"} has a message for you, check it out!")
                //logger.debug { "Tagging users : $users" }
                return tags.toString()
            }
            //logger.debug { "No users selected from groups $groupNames" }
            return null
        }
    }

}
