package org.koppakurhiev.janabot.services.patVpat.data

data class Subscriber(
    val chatId: Long,
    val username: String,
    val reminders: Boolean,
)
