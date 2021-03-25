package org.koppakurhiev.janabot.telegram.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotService
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.koppakurhiev.janabot.telegram.services.ABotService

class SubGroupsService(bot: ITelegramBot) : ABotService(bot) {

    private val regex = Regex("@[a-zA-Z0-9_]+")

    val subGroupsManager = SubGroupsManager(bot)

    override fun getCommands(): Array<IBotService.IBotCommand> {
        return arrayOf(
            TagCommand(this),
            GroupCommand(this),
        )
    }

    override suspend fun onMessage(message: Message) {
        if (message.text != null) {
            val conversation = Conversation(bot, message)
            val matches = regex.findAll(message.text.toString())
            val taggedChannels = mutableListOf<String>()
            matches.forEach { taggedChannels.add(it.value.drop(1)) }
            val text = TagCommand.tagMembers(
                subGroupsManager,
                message.chat.id,
                *taggedChannels.toTypedArray()
            )
            if (text != null) conversation.replyMessage(text)
        }
    }
}
