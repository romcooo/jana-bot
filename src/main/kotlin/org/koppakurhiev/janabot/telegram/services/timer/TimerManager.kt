package org.koppakurhiev.janabot.telegram.services.timer

import org.koppakurhiev.janabot.common.Duration
import org.koppakurhiev.janabot.common.getLogger
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.litote.kmongo.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

class TimerManager(val bot: ITelegramBot) {
    private val logger = getLogger()

    private fun save(data: TimerData): OperationResult {
        val collection = bot.database.getCollection<TimerData>()
        val result = collection.updateOne(data, upsert())
        return if (result.wasAcknowledged())
            OperationResult.SUCCESS else {
            logger.error { "Timer save failed!" }
            OperationResult.SAVE_FAILED
        }
    }

    fun getReaction(type: TimerReaction.Type): String? {
        val collection = bot.database.getCollection<TimerReaction>()
        val reactions = collection.find(TimerReaction::type eq type)
        val size = reactions.count()
        return if (size > 0) {
            val randomIndex = Random.nextInt(0, size)
            reactions.skip(randomIndex).first()?.text
        } else {
            null
        }
    }

    fun addReaction(type: TimerReaction.Type, text: String): OperationResult {
        val collection = bot.database.getCollection<TimerReaction>()
        val reaction = TimerReaction(type = type, text = text)
        logger.info { "Adding timer reaction '$text'" }
        return if (collection.insertOne(reaction).wasAcknowledged()) {
            OperationResult.SUCCESS
        } else {
            OperationResult.SAVE_FAILED
        }
    }

    fun getData(chatId: Long): TimerData? {
        val collection = bot.database.getCollection<TimerData>()
        return collection.findOne { TimerData::chatId eq chatId }
    }

    fun resetTimer(chatId: Long): OperationResult {
        val timerData = getData(chatId)
        if (timerData == null) {
            val saveResult = save(TimerData(chatId = chatId))
            logger.info { "New timer initiated" }
            return if (saveResult != OperationResult.SUCCESS) saveResult else OperationResult.INITIATED
        }
        val now = LocalDateTime.now()
        val lastReset = timerData.timer
        val lastRun = Duration.between(lastReset.toEpochSecond(ZoneOffset.UTC), now.toEpochSecond(ZoneOffset.UTC))
        timerData.timer = now
        timerData.lastRunLength = lastRun
        val oldRecord = timerData.record
        logger.info { "$chatId - Timer reset after $lastRun sec." }
        if (oldRecord != null && oldRecord < lastRun) {
            timerData.record = lastRun
            return if (save(timerData) == OperationResult.SUCCESS)
                OperationResult.RECORD_BROKEN else OperationResult.SAVE_FAILED
        }
        return save(timerData)
    }

    enum class OperationResult {
        SAVE_FAILED,
        RECORD_BROKEN,
        INITIATED,
        SUCCESS
    }
}