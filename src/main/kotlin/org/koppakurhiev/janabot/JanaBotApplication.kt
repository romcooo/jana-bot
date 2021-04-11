package org.koppakurhiev.janabot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.koppakurhiev.janabot.common.ALogged
import org.koppakurhiev.janabot.common.getArg
import org.koppakurhiev.janabot.persistence.MongoRepository
import org.koppakurhiev.janabot.telegram.bot.KoppaBot

object BotRunner : ALogged() {
    val bots: Set<IBot> = setOf(
        KoppaBot(),
    )

    suspend fun launch() {
        val jobs = mutableListOf<Job>()
        bots.forEach {
            jobs.add(GlobalScope.launch {
                try {
                    it.launch()
                } catch (exception: Exception) {
                    logger.error("The bot ${it.name} failed to start", exception)
                }
            })
        }
        jobs.joinAll()
    }
}

suspend fun main() {
    BotRunner.launch()
    CommandLineInterface.launch()
}

private object CommandLineInterface : ALogged() {

    var mainJob: Job? = null

    fun launch() {
        mainJob = GlobalScope.launch {
            readCmdLine()
        }
    }

    private suspend fun readCmdLine() {
        var command: List<String> = emptyList()
        logger.info { "Listening for command line commands" }
        while (command.getArg(0) != "stop") {
            command = readLine()!!.split(" ")
            when (command.getArg(0)) {
                "stop" -> onStopCommand()
            }
        }
    }

    fun getBotForName(botName: String): IBot? {
        return BotRunner.bots.find { it.name == botName }
    }

    suspend fun onStopCommand() {
        logger.info { "The app is shutting down" }
        val jobs = mutableListOf<Job>()
        BotRunner.bots.forEach {
            jobs.add(GlobalScope.launch {
                it.stop()
            })
        }
        jobs.joinAll()
        MongoRepository.close()
    }
}