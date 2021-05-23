package org.koppakurhiev.janabot.telegram.services.patVpat.data

data class Subscriber(
    val chatId: Long,
    val username: String,
    var reminders: Boolean = true,
)
