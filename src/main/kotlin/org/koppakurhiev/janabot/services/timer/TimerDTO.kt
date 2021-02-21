package org.koppakurhiev.janabot.services.timer

import org.koppakurhiev.janabot.persistence.KlaxonDuration
import org.koppakurhiev.janabot.persistence.KlaxonLocalDateTime
import java.time.Duration
import java.time.LocalDateTime

data class TimerDTO @JvmOverloads constructor(
    @KlaxonLocalDateTime var timer: LocalDateTime = LocalDateTime.now(),
    @KlaxonDuration var lastRunLength: Duration = Duration.ZERO,
    @KlaxonDuration var record: Duration = Duration.ZERO
)
