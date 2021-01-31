package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.LivingMessage
import org.koppakurhiev.janabot.sendMessageWithLifetime
import org.koppakurhiev.janabot.services.ABotService

class TagCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/tag") {
    override fun onCommand(message: Message, s: String?) {
        var words = message.text?.split(" ")
        words = words?.subList(1, words.size)
        if (words != null) {
            val text = tagMembers(subGroupsManager, message.from?.username, message.chat.id, *words.toTypedArray())
            JanaBot.bot.sendMessageWithLifetime(message.chat.id, text ?: "No people selected with given groups: $words", lifetime = LivingMessage.MessageLifetime.FOREVER)
        } else {
            JanaBot.bot.sendMessageWithLifetime(
                message.chat.id, "Invalid command format!\n" +
                        "Use /tag <group-name> .."
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
