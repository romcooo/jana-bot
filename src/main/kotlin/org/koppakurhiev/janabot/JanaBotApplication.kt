package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import mu.KotlinLogging
import org.koppakurhiev.janabot.services.DefaultServices
import org.koppakurhiev.janabot.services.IBotService
import org.koppakurhiev.janabot.services.subgroups.SubGroupsService

fun main() {
    JanaBot.launch()
}

object JanaBot {
    private val logger = KotlinLogging.logger {}
    lateinit var bot: Bot

    //Add services here when implemented
    val services = arrayOf<IBotService>(
        DefaultServices(),
        SubGroupsService(),
    )

    fun launch() {
        val botBuilder = BotBuilder()
        bot = botBuilder
            .withServices(services)
            .build()
        bot.start()
        logger.info("JanaBot started.")
    }
}
