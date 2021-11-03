package org.koppakurhiev.janabot.telegram.services.patVpat.data

import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class Question @JvmOverloads constructor(
    val _id: Id<Question> = newId(),
    val text: String,
    val creator: String,
    var skipped: Boolean = false,
    var asked: Boolean = false,
    var exported: Boolean = false
)