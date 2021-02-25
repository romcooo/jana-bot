package org.koppakurhiev.janabot.services.patVpat.data

import java.time.LocalDateTime

data class PatVPatData(
    var idCounter: Long = 0,
    var runningQuestion: Question? = null,
    var subscribers: MutableList<Subscriber> = mutableListOf(),
    var answers: MutableList<Answer> = mutableListOf(),
    var nextQuestionAt: LocalDateTime? = null,
    var reminderAt: LocalDateTime? = null,
    var questions: MutableList<Question>? = null
)


