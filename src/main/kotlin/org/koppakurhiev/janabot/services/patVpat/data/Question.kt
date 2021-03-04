package org.koppakurhiev.janabot.services.patVpat.data

data class Question(
    val id: Long,
    val text: String,
    val creator: String,
    var skipped: Boolean = false,
    var asked: Boolean = false,
    var exported: Boolean = false
)