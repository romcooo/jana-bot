package org.koppakurhiev.janabot.common

import java.util.*

object JobScheduler {
    private val timer = Timer()

    fun schedule(task: TimerTask, delay: Long) {
        timer.schedule(task, delay)
    }
}