package org.koppakurhiev.janabot.services.patVpat.data

import com.beust.klaxon.Klaxon
import org.koppakurhiev.janabot.persistence.AFileRepository
import java.io.File

class PatVPatRepository : AFileRepository<PatVPatData>("5v5", "subscribers") {
    override fun load(filePath: String): PatVPatData? {
        logger.info { "Loading data from $filePath" }
        return Klaxon().parse(File(filePath))
    }
}