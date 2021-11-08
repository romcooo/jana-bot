package org.koppakurhiev.janabot.telegram.services.subgroups

import com.elbekD.bot.types.Message
import org.jetbrains.annotations.PropertyKey
import org.koppakurhiev.janabot.common.AStringsProvider
import org.koppakurhiev.janabot.common.Strings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotService
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot

class SubGroupsService(override val bot: ITelegramBot) : IBotService {

    override val commands = arrayOf(
        SubGroupCommand(this),
        TagCommand(this),
    )

    val subGroupsManager = SubGroupsManager(bot)

    private val regex = Regex("@[a-zA-Z0-9_]+")

    override suspend fun onMessage(message: Message) {
        if (message.text != null) {
            val conversation = Conversation(bot, message)
            val matches = regex.findAll(message.text.toString())
            val taggedChannels = mutableListOf<String>()
            matches.forEach { taggedChannels.add(it.value.drop(1)) }
            val text = TagCommand.getTagMessage(
                subGroupsManager,
                conversation,
                *taggedChannels.toTypedArray()
            )
            if (text != null) conversation.replyMessage(text)
        }
    }
}

object SubGroupStrings : AStringsProvider("/SubGroup") {
    fun getString(
        locale: Strings.Locale = Strings.Locale.DEFAULT,
        @PropertyKey(resourceBundle = "SubGroup") key: String,
        vararg args: Any?
    ): String {
        return getStringsForLocale(locale).get(key, *args)
    }
}