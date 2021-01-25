package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import mu.KotlinLogging
import org.koppakurhiev.janabot.commands.Help
import org.koppakurhiev.janabot.commands.Start
import org.koppakurhiev.janabot.services.SubGroupsService

fun main() {
    JanaBot.launch()
}

object JanaBot {
    private val logger = KotlinLogging.logger {}
    lateinit var bot: Bot

    private val services = arrayOf<IBotService>(
        SubGroupsService(),
    )

    private val defaultCommands = arrayOf<ICommand>(
        Help(services),
        Start(),
    )

    fun launch() {
        val botBuilder = BotBuilder()
        bot = botBuilder
            .withCommands(defaultCommands)
            .withServices(services)
            .build()
        bot.start()
        logger.info("JanaBot started.")
    }
}