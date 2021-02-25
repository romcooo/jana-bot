package org.koppakurhiev.janabot.services.patVpat.data

import com.beust.klaxon.Klaxon
import org.koppakurhiev.janabot.persistence.AFileRepository
import java.io.File

class AnswersRepository : AFileRepository<List<Answer>>("5v5", "answers") {
    override fun load(filePath: String): List<Answer>? {
        logger.info { "Loading data from $filePath" }
        return Klaxon().parseArray(File(filePath))
    }
}