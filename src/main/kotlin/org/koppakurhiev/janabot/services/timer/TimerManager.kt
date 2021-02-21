package org.koppakurhiev.janabot.services.timer

import org.koppakurhiev.janabot.utils.ALogged
import java.time.Duration
import java.time.LocalDateTime

class TimerManager : ALogged() {
    lateinit var timerData: TimerDTO
    private val repository = TimerRepository()

    init {
        if (load() != OperationResult.SUCCESS) {
            logger.error { "Timer data loading failed!" }
        }
    }

    private fun load(): OperationResult {
        logger.info { "Loading timer data" }
        val data = repository.load()
        if (data == null) {
            logger.warn { "Data not found, initializing the timer" }
            timerData = TimerDTO()
            return save()
        }
        timerData = data
        return OperationResult.SUCCESS
    }

    private fun save(): OperationResult {
        return if (repository.save(timerData))
            OperationResult.SUCCESS else OperationResult.SAVE_FAILED
    }

    fun resetTimer(): OperationResult {
        val now = LocalDateTime.now()
        val lastReset = timerData.timer
        val lastRun = Duration.between(lastReset, now)
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