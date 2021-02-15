package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import org.koppakurhiev.janabot.features.StringProvider
import org.koppakurhiev.janabot.services.DefaultServices
import org.koppakurhiev.janabot.services.IBotService
import org.koppakurhiev.janabot.services.subgroups.SubGroupsService
import org.koppakurhiev.janabot.utils.ALogged
import org.koppakurhiev.janabot.utils.BotBuilder
import java.util.*

fun main() {
    JanaBot.launch()
}

object JanaBot : ALogged() {
    lateinit var bot: Bot
    lateinit var messages: StringProvider
    val properties = Properties()

    //Add services here when implemented
    fun getServices(): Array<IBotService> {
        return arrayOf(
            DefaultServices(),
            SubGroupsService(),
        )
    }

    fun launch() {
        val configStream = javaClass.getResourceAsStream("/config.properties")
        properties.load(configStream)
        configStream.close()
        val botBuilder = BotBuilder(properties)
        bot = botBuilder
            .withServices(getServices())
            .build()
        messages = StringProvider("en")
        bot.start()
        logger.info("JanaBot started.")
    }
}
