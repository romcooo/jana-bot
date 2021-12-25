package org.koppakurhiev.janabot.telegram.services.patVpat.export

import com.google.api.client.util.ArrayMap
import org.koppakurhiev.janabot.common.getLogger
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.koppakurhiev.janabot.telegram.services.patVpat.data.Answer
import org.koppakurhiev.janabot.telegram.services.patVpat.data.Question
import org.litote.kmongo.*

class PatVPatExportManager(val bot: ITelegramBot) {

    private val logger = getLogger()

    private val answersCollection = bot.database.getCollection<Answer>("5v5_answers")
    private val questionsCollection = bot.database.getCollection<Question>("5v5_questions")
    private val sheets = SheetsRepository(bot)

    fun exportQuestions(sheetId: String): Boolean {
        val questions = questionsCollection.find().sortedBy { it.text }
        return if (sheets.saveAll(sheetId, questions.toList())) {
            logger.info { "All 5v5 questions has been exported" }
            true
        } else {
            logger.error { "5v5 questions export failed" }
            false
        }
    }

    fun exportAllWithAnswers(sheetId: String, exported: Boolean): Boolean {
        var questions = questionsCollection
            .find(Question::asked eq true, Question::skipped eq false)
            .toList()
        if (!exported) {
            questions = questions.filter { !it.exported }
        }
        val map = ArrayMap<Question, List<Answer>>()
        questions.shuffled().forEach {
            val answers = answersCollection.find(Answer::questionTag eq it._id).toList()
            map.add(it, answers)
        }
        return if (sheets.saveAll(sheetId, map)) {
            logger.info { "All questions exported, exported: $exported" }
            markQuestionsExported(questions)
            true
        } else {
            logger.error { "Export of questions with answers failed, exported: $exported" }
            false
        }
    }

    fun exportAnswers(sheetId: String, n: Int): Boolean {
        if (n <= 0) {
            return false
        }
        var questions = questionsCollection.find(
            Question::exported eq false,
            Question::asked eq true,
            Question::skipped eq false
        ).toList()
        questions = questions.shuffled().take(n)
        val map = ArrayMap<Question, List<Answer>>()
        questions.forEach {
            val answers = answersCollection.find(Answer::questionTag eq it._id).toList()
            map.add(it, answers)
        }
        return if (sheets.saveAll(sheetId, map)) {
            logger.info { "${questions.size} questions exported" }
            markQuestionsExported(questions)
            true
        } else {
            logger.error { "Export of $n questions with answers failed" }
            false
        }
    }

    private fun markQuestionsExported(questions: List<Question>) {
        questions.forEach { it.exported = true }
        questionsCollection.updateMany(
            Question::_id `in` questions.map { it._id },
            set(Question::exported setTo true)
        )
    }

}