package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import org.koppakurhiev.janabot.services.DefaultServices
import org.koppakurhiev.janabot.services.IBotService
import org.koppakurhiev.janabot.services.subgroups.SubGroupsService
import org.koppakurhiev.janabot.utils.ALogged
import org.koppakurhiev.janabot.utils.BotBuilder
import java.io.FileInputStream
import java.util.*

fun main() {
    JanaBot.launch()
}

object JanaBot : ALogged() {
    lateinit var bot: Bot
    val properties = Properties()

    //Add services here when implemented
    val services = arrayOf<IBotService>(
        DefaultServices(),
        SubGroupsService(),
    )

    fun launch() {
        properties.load(FileInputStream("src/main/resources/config.properties"))
        val botBuilder = BotBuilder(properties)
        bot = botBuilder
            .withServices(services)
            .build()
        bot.start()
        logger.info("JanaBot started.")
    }
}
