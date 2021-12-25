package org.koppakurhiev.janabot.telegram.services.patVpat.export

import org.koppakurhiev.janabot.IBot
import org.koppakurhiev.janabot.persistence.ASheetsRepository
import org.koppakurhiev.janabot.telegram.services.patVpat.data.Answer
import org.koppakurhiev.janabot.telegram.services.patVpat.data.Question

class SheetsRepository(bot: IBot) : ASheetsRepository<Question>(bot) {

    override fun parse(values: List<List<Any>>): Question? {
        throw UnsupportedOperationException()
    }

    fun save(sheetId: String, data: Question): Boolean {
        return save(
            sheetId,
            values = listOf(
                listOf(
                    data.text,
                    data.asked.toString(),
                    data.skipped.toString(),
                    data.exported.toString()
                )
            )
        )
    }

    fun saveAll(sheetId: String, data: List<Question>): Boolean {
        val formatted = mutableListOf<List<String>>()
        data.forEach { formatted.add(listOf(it.text, it.exported.toString())) }
        return save(sheetId, values = formatted)
    }

    fun saveAll(sheetId: String, data: Map<Question, List<Answer>>): Boolean {
        val formatted = mutableListOf<List<String>>()
        data.forEach { (question, answers) ->
            formatted.add(listOf(question.text))
            answers.forEach { formatted.add(listOf("", "", it.text)) }
        }
        return save(sheetId, values = formatted)
    }
}