package org.koppakurhiev.janabot.utils

import com.elbekD.bot.Bot
import com.elbekD.bot.types.Message
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
        }
        return this
    }

    fun onMessage(action: suspend ((message: Message) -> Unit)): BotBuilder {
        bot.onMessage(action)
        return this
    }

    fun build(): Bot {
        return bot
    }

    enum class ConstructionType {
        POLLING, WEBHOOK
    }
}