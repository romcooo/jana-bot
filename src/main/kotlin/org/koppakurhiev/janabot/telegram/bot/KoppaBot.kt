package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.types.CallbackQuery
import org.koppakurhiev.janabot.common.getLogger
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.services.DefaultCommandsService
import org.koppakurhiev.janabot.telegram.services.backpack.BackpackService
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
                BackpackService(this),
            )
        )
    }

    override suspend fun stop() {
        getLogger().info { "Deconstructing bot ${properties.getProperty("bot.username")}" }
        deconstructServices()
        telegramBot.stop()
        getLogger().info("${properties.getProperty("bot.username")} stopped.")
    }

    override suspend fun launch() {
        getLogger().info { "Building bot ${properties.getProperty("bot.username")}" }
        buildServices()
        buildBot()
        initializeServices()
        getLogger().info("${properties.getProperty("bot.username")} started.")
    }

    override fun onCallbackQueryWithNoData(query: CallbackQuery) {
        telegramBot.answerCallbackQuery(query.id, TelegramStrings.getString(key = "callback.unresolved"))
    }
}