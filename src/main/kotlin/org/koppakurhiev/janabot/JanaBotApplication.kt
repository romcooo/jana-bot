package org.koppakurhiev.janabot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.koppakurhiev.janabot.common.ALogged
import org.koppakurhiev.janabot.telegram.bot.KoppaBot

object BotRunner : ALogged() {
    private val bots: Set<IBot> = setOf(
        KoppaBot(),
    )

    suspend fun launch() {
        val jobs = mutableListOf<Job>()
        bots.forEach {
            jobs.add(GlobalScope.launch {
                try {
                    it.launch()
                } catch (exception: Exception) {
                    logger.error("The bot ${it.getName()} failed to start", exception)
                }
            })
        }
        jobs.joinAll()
    }
}

suspend fun main() {
    BotRunner.launch()
}
