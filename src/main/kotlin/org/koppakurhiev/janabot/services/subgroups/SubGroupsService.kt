package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.services.IBotService
import org.koppakurhiev.janabot.utils.sendMessage

class SubGroupsService : ABotService() {
    private val subGroupsManager = SubGroupsManager()
    private val regex = Regex("@[a-zA-Z0-9_]+")

    override fun help(): String {
        logger.trace { "SubGroup help called" }
        return JanaBot.messages.get("group.help")
    }

    override fun getCommands(): Array<IBotService.ICommand> {
        return arrayOf(
            GroupCommand(subGroupsManager),
            TagCommand(subGroupsManager),
        )
    }

    override suspend fun onMessage(message: Message) {
        if (message.text != null) {
            val matches = regex.findAll(message.text.toString())
            val taggedChannels = mutableListOf<String>()
            matches.forEach { taggedChannels.add(it.value.drop(1)) }
            val text = TagCommand.tagMembers(
                subGroupsManager,
                message.chat.id,
                *taggedChannels.toTypedArray()
            )
            text?.let {
                JanaBot.bot.sendMessage(
                    message.chat.id,
                    text,
                    replyTo = message.message_id,
                    lifetime = MessageLifetime.FOREVER
                )
            }
        }
    }
}
