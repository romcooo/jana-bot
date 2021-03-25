package org.koppakurhiev.janabot.telegram.services.timer

import org.koppakurhiev.janabot.IBot
import org.koppakurhiev.janabot.common.ALogged
import org.koppakurhiev.janabot.common.Duration
import org.litote.kmongo.getCollection
import org.litote.kmongo.replaceOne
import java.time.LocalDateTime
import java.time.ZoneOffset

class TimerManager(val bot: IBot) : ALogged() {
    lateinit var timerData: TimerData

    init {
        if (load() != OperationResult.SUCCESS) {
            logger.error { "Timer data loading failed!" }
        }
    }

    private fun load(): OperationResult {
        logger.info { "Loading timer data" }
        val collection = bot.getDatabase().getCollection<TimerData>()
        val data = collection.find().first()
        if (data == null) {
            logger.warn { "Data not found, initializing the timer" }
            timerData = TimerData()
            val result = collection.insertOne(timerData)
            return if (!result.wasAcknowledged()) {
                OperationResult.SAVE_FAILED
            } else {
                OperationResult.SUCCESS
            }
        } else {
            timerData = data
            return OperationResult.SUCCESS
        }
    }

    private fun save(): OperationResult {
        val collection = bot.getDatabase().getCollection<TimerData>()
        val result = collection.replaceOne(timerData)
        return if (result.wasAcknowledged())
            OperationResult.SUCCESS else OperationResult.SAVE_FAILED
    }

    fun resetTimer(): OperationResult {
        val now = LocalDateTime.now()
        val lastReset = timerData.timer
        val lastRun = Duration.between(lastReset.toEpochSecond(ZoneOffset.UTC), now.toEpochSecond(ZoneOffset.UTC))
        timerData.timer = now
        timerData.lastRunLength = lastRun
        if (timerData.record < lastRun) {
            timerData.record = lastRun
            return if (save() == OperationResult.SUCCESS)
                OperationResult.RECORD_BROKEN else OperationResult.SAVE_FAILED
        }
        return save()
    }

    enum class OperationResult {
        SAVE_FAILED,
        RECORD_BROKEN,
        SUCCESS
    }
}