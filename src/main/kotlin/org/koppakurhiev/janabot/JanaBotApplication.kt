package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import org.koppakurhiev.janabot.services.DefaultServices
import org.koppakurhiev.janabot.services.IBotService
import org.koppakurhiev.janabot.services.subgroups.SubGroupsService
import org.koppakurhiev.janabot.utils.ALogged
import org.koppakurhiev.janabot.utils.BotBuilder

fun main() {
    JanaBot.launch()
}

object JanaBot : ALogged() {
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
