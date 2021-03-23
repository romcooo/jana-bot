package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.features.StringProvider
import org.koppakurhiev.janabot.persistence.MongoRepository
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

    init {
        val configStream = javaClass.getResourceAsStream("/config.properties")
        properties.load(configStream)
        configStream.close()
    }

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
        logger.info { "Building bot ${properties.getProperty("bot.username")}" }
        MongoRepository.initialize()
        messages = StringProvider("en")
        buildServices()
        bot = BotBuilder(properties)
            .withServices(services)
            .onMessage(this::onMessage)
            .build()
        bot.start()
        logger.info("${properties.getProperty("bot.username")} started.")
    }

    private suspend fun onMessage(message: Message) {
        services.forEach {
            it.onMessage(message)
        }
    }

    fun isAdmin(username: String?): Boolean {
        if (username == null) return false
        val admins = properties.getProperty("admins").split(", ")
        return admins.contains(username)
    }
}
