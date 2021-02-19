package org.koppakurhiev.janabot.services.subgroups

import org.koppakurhiev.janabot.persistence.ARepository

class SubGroupRepository(directoryName: String, fileName: String) :
    ARepository<SubGroup>(directoryName, fileName) {
    override fun load(filePath: String): List<SubGroup>? {
        logger.info { "Loading data from $filePath" }
        return parse(filePath)
    }
}