package org.koppakurhiev.janabot.services.patVpat.data


import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.time.LocalDateTime

data class PatVPatData @JvmOverloads constructor(
    val _id: Id<PatVPatData> = newId(),
    var runningQuestion: Question? = null,
    val subscribers: MutableList<Subscriber> = mutableListOf(),
    var nextQuestionAt: LocalDateTime? = null,
    var reminderAt: LocalDateTime? = null
)


