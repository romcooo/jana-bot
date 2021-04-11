package org.koppakurhiev.janabot.telegram.bot

import org.koppakurhiev.janabot.common.Strings
import org.koppakurhiev.janabot.telegram.services.backpack.Backpack
import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class ChatData @JvmOverloads constructor(
    val _id: Id<ChatData> = newId(),
    var language: Strings.Locale = Strings.Locale.DEFAULT,
    val chatId: Long,
    var backpacks: MutableSet<Backpack> = mutableSetOf()
)