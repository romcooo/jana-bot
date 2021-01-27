package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import org.koppakurhiev.janabot.services.IBotService

class BotBuilder(constType: ConstructionType = ConstructionType.POOLING) {

    companion object {
        private var BOT_USERNAME = System.getenv("janaBotUsername")
        private var BOT_TOKEN = System.getenv("janaBotToken")
    }

    private val bot: Bot = when (constType) {
        ConstructionType.POOLING -> Bot.createPolling(BOT_USERNAME, BOT_TOKEN)
        ConstructionType.WEBHOOK -> TODO("Implement web hook bot creation")
    }

    private fun registerCommands(commands: Array<IBotService.ICommand>) {
        commands.forEach {
            bot.onCommand(it.trigger, it::onCommand)
        }
    }

    fun withServices(services: Array<IBotService>): BotBuilder {
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
        POOLING, WEBHOOK
    }
}