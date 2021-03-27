package org.koppakurhiev.janabot.telegram.services.patVpat.data


import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.time.LocalDateTime

data class PatVPatData @JvmOverloads constructor(
    val _id: Id<PatVPatData> = newId(),
    var questionId: Id<Question>? = null,
    val subscribers: MutableList<Subscriber> = mutableListOf(),
    var nextQuestionAt: LocalDateTime? = null,
    var reminderAt: LocalDateTime? = null
)


