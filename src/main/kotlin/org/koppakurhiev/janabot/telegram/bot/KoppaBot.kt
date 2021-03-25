package org.koppakurhiev.janabot.telegram.bot

import org.koppakurhiev.janabot.telegram.services.DefaultCommandsService
import org.koppakurhiev.janabot.telegram.services.patVpat.PatVPatService
import org.koppakurhiev.janabot.telegram.services.subgroups.SubGroupsService
import org.koppakurhiev.janabot.telegram.services.timer.TimerService

class KoppaBot : ATelegramBot("KoppaTelegram") {

    /**
     *  Add services here
     */
    private fun buildServices() {
        services.addAll(
            setOf<IBotService>(
                DefaultCommandsService(this),
                SubGroupsService(this),
                TimerService(this),
                PatVPatService(this),
            )
        )
    }

    override suspend fun stop() {
        deconstructServices()
    }

    override suspend fun launch() {
        logger.info { "Building bot ${getProperties().getProperty("bot.username")}" }
        buildServices()
        buildBot()
        initializeServices()
        logger.info("${getProperties().getProperty("bot.username")} started.")
    }
}