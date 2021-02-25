package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import org.koppakurhiev.janabot.features.StringProvider
import org.koppakurhiev.janabot.services.DefaultServices
import org.koppakurhiev.janabot.services.IBotService
import org.koppakurhiev.janabot.services.patVpat.PatVPatService
import org.koppakurhiev.janabot.services.subgroups.SubGroupsService
import org.koppakurhiev.janabot.services.timer.TimerService
import org.koppakurhiev.janabot.utils.ALogged
import org.koppakurhiev.janabot.utils.BotBuilder
import java.util.*

fun main() {
    JanaBot.launch()
}

object JanaBot : ALogged() {
    lateinit var bot: Bot
    lateinit var messages: StringProvider
    lateinit var services: Set<IBotService>
    val properties = Properties()

    //Add services here when implemented
    private fun buildServices() {
        val localServices = setOf(
            DefaultServices(),
            SubGroupsService(),
            TimerService(),
            PatVPatService(),
        )
        services = localServices
    }

    fun launch() {
        val configStream = javaClass.getResourceAsStream("/config.properties")
        //IMPORTANT be careful with initialization order
        properties.load(configStream)
        configStream.close()
        logger.info { "Building bot ${properties.getProperty("bot.username")}" }
        messages = StringProvider("en")
        buildServices()
        bot = BotBuilder(properties)
            .withServices(services)
            .build()
        bot.start()
        logger.info("${properties.getProperty("bot.username")} started.")
    }
}
