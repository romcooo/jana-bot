package org.koppakurhiev.janabot.services.patVpat.data

import com.beust.klaxon.Json
import org.koppakurhiev.janabot.persistence.KlaxonLocalDateTime
import java.time.LocalDateTime

data class PatVPatData @JvmOverloads constructor(
    var idCounter: Long = 0,
    var runningQuestion: Question? = null,
    val subscribers: MutableList<Subscriber> = mutableListOf(),
    val answers: MutableList<Answer> = mutableListOf(),
    @KlaxonLocalDateTime
    var nextQuestionAt: LocalDateTime? = null,
    @KlaxonLocalDateTime
    var reminderAt: LocalDateTime? = null,
    @Json(ignored = true)
    var questions: MutableList<Question>? = null
)


