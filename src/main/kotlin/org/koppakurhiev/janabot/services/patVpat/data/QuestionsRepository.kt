package org.koppakurhiev.janabot.services.patVpat.data

import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.persistence.ASheetsRepository
import java.io.IOException
import java.security.GeneralSecurityException

class QuestionsRepository(private val questionsPage: String) : ASheetsRepository<List<Question>>() {

    override fun save(data: List<Question>, backup: Boolean): Boolean {
        val values: MutableList<List<String>> = mutableListOf()
        data.forEach {
            val row = listOf(
                it.id.toString(),
                it.creator,
                it.text,
                it.asked.toString(),
                it.skipped.toString(),
                it.exported.toString()
            )
            values.add(row)
        }
        return save(getSheetId(), "$questionsPage!A2:F", values)
    }

    override fun load(sourceIndex: Int?): List<Question>? {
        try {
            return super.load(getSheetId(), questionsPage) ?: listOf()
        } catch (e: IOException) {
            logger.error { e }
        } catch (e: GeneralSecurityException) {
            logger.error { e }
        }
        return null
    }

    override fun parse(values: List<List<Any>>): List<Question> {
        logger.trace { "Parsing values: $values" }
        val relevant = values.filter { it[0] != "ID" && it[0].toString().isNotBlank() }
        val questions = mutableListOf<Question>()
        relevant.forEach {
            val id = it[0].toString().toLong()
            val creator = it[1].toString()
            val text = it[2].toString()
            val asked = it[3].toString().toBoolean()
            val skipped = it[4].toString().toBoolean()
            val exported = it[5].toString().toBoolean()
            val question = Question(id, text, creator, skipped, asked, exported)
            questions.add(question)
        }
        return questions
    }

    override fun getAvailableBackups(): List<String> {
        return emptyList()
    }

    private fun getSheetId(): String {
        return JanaBot.properties.getProperty("sheets.5v5Sheet")
    }
}