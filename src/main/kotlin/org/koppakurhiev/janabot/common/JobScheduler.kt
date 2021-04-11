package org.koppakurhiev.janabot.common

import java.util.*
import kotlin.concurrent.schedule

object JobScheduler {
    private val timer = Timer()

    fun schedule(task: TimerTask, delay: Long) {
        timer.schedule(task, delay)
    }

    fun schedule(delay: Long, action: TimerTask.() -> Unit) {
        timer.schedule(delay, action)
    }

}