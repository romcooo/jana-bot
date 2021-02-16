package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.services.IBotService

class SubGroupsService : ABotService() {
    private val subGroupsManager = SubGroupsManager()
    private val regex = Regex("@[a-zA-Z0-9_]+")

    override fun getCommands(): Array<IBotService.ICommand> {
        return arrayOf(
            TagCommand(subGroupsManager),
            GroupCommand(subGroupsManager),
            BackupCommand(subGroupsManager),
        )
    }

    override suspend fun onMessage(message: Message) {
        if (message.text != null) {
            val conversation = Conversation(message)
            val matches = regex.findAll(message.text.toString())
            val taggedChannels = mutableListOf<String>()
            matches.forEach { taggedChannels.add(it.value.drop(1)) }
            val text = TagCommand.tagMembers(
                subGroupsManager,
                message.chat.id,
                *taggedChannels.toTypedArray()
            )
            if (text != null) conversation.sendMessage(text)
        }
    }
}
