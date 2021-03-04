package org.koppakurhiev.janabot.services.subgroups

import com.beust.klaxon.Klaxon
import org.koppakurhiev.janabot.persistence.AFileRepository
import java.io.File

class SubGroupRepository : AFileRepository<List<SubGroup>>("groups", "groups") {
    override fun load(filePath: String): List<SubGroup>? {
        logger.info { "Loading data from $filePath" }
        return Klaxon().parseArray(File(filePath))
    }
}