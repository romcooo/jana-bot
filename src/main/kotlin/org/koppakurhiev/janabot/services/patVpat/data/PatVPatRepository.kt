package org.koppakurhiev.janabot.services.patVpat.data

import com.beust.klaxon.Klaxon
import org.koppakurhiev.janabot.persistence.AFileRepository
import org.koppakurhiev.janabot.persistence.KlaxonLocalDateTime
import org.koppakurhiev.janabot.persistence.LocalDateTimeConverter
import java.io.File

class PatVPatRepository : AFileRepository<PatVPatData>("5v5", "5v5Data") {
    override fun load(filePath: String): PatVPatData? {
        logger.info { "Loading data from $filePath" }
        return Klaxon()
            .fieldConverter(KlaxonLocalDateTime::class, LocalDateTimeConverter)
            .parse(File(filePath))
    }
}