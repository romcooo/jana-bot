package org.koppakurhiev.janabot.services.timer

import org.koppakurhiev.janabot.utils.Duration
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.time.LocalDateTime

data class TimerData @JvmOverloads constructor(
    val _id: Id<TimerData> = newId(),
    var timer: LocalDateTime = LocalDateTime.now(),
    var lastRunLength: Duration = Duration(),
    var record: Duration = Duration()
)
