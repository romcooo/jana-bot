package org.koppakurhiev.janabot.services.patVpat.data

import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class Answer @JvmOverloads constructor(
    val _id: Id<Answer> = newId(),
    var questionTag: Id<Question>? = null,
    val chatId: Long,
    var text: String
)
