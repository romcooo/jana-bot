package org.koppakurhiev.janabot.services.patVpat.data

import org.koppakurhiev.janabot.persistence.ASheetsRepository

class QuestionsRepository : ASheetsRepository<List<Question>>() {
    override fun save(data: List<Question>, backup: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun load(sourceIndex: Int?): List<Question>? {
        TODO("Not yet implemented")
    }

    override fun getAvailableBackups(): List<String> {
        return emptyList()
    }
}