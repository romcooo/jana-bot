package org.koppakurhiev.janabot.services.patVpat.data

data class Question(
    val id: Long,
    val text: String,
    val creator: String,
    val skipped: Boolean = false,
    val asked: Boolean = false,
    val exported: Boolean = false
)