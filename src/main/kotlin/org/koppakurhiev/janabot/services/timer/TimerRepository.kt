package org.koppakurhiev.janabot.services.timer

import com.beust.klaxon.Klaxon
import org.koppakurhiev.janabot.persistence.*
import java.io.File

class TimerRepository : ARepository<TimerDTO>("timer", "timerData") {
    override fun load(filePath: String): TimerDTO? {
        logger.info { "Getting timer data from: $filePath" }
        return Klaxon()
            .fieldConverter(KlaxonDuration::class, DurationConverter)
            .fieldConverter(KlaxonLocalDateTime::class, LocalDateTimeConverter)
            .parse(File(filePath))
    }
}