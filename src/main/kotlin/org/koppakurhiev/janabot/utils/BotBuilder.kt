package org.koppakurhiev.janabot.utils

import com.elbekD.bot.Bot
import org.koppakurhiev.janabot.services.IBotService
import java.util.*

class BotBuilder(properties: Properties, constType: ConstructionType = ConstructionType.POLLING) {

    private val bot: Bot = when (constType) {
        ConstructionType.POLLING -> Bot.createPolling(
            properties.getProperty("bot.username"),
            properties.getProperty("bot.token")
        )
        ConstructionType.WEBHOOK -> TODO("Implement web hook bot creation")
    }

    private fun registerCommands(commands: Array<IBotService.ICommand>) {
        commands.forEach {
            bot.onCommand(it.trigger, it::onCommand)
        }
    }

    fun withServices(services: Set<IBotService>): BotBuilder {
        services.forEach {
            registerCommands(it.getCommands())
            bot.onMessage(it::onMessage)
        }
        return this
    }

    fun build(): Bot {
        return bot
    }

    enum class ConstructionType {
        POLLING, WEBHOOK
    }
}