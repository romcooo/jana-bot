package org.koppakurhiev.janabot.services.patVpat.data

import com.beust.klaxon.Klaxon
import org.koppakurhiev.janabot.persistence.AFileRepository
import java.io.File

class SubscribersRepository : AFileRepository<List<Subscriber>>("5v5", "subscribers") {
    override fun load(filePath: String): List<Subscriber>? {
        logger.info { "Loading data from $filePath" }
        return Klaxon().parseArray(File(filePath))
    }
}