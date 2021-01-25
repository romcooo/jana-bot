package org.koppakurhiev.janabot

import com.elbekD.bot.Bot

private var BOT_USERNAME = System.getenv("janaBotUsername")
private var BOT_TOKEN = System.getenv("janaBotToken")

class BotBuilder(constType: ConstructionType = ConstructionType.POOLING) {

    private val bot: Bot = when (constType) {
        ConstructionType.POOLING -> Bot.createPolling(BOT_USERNAME, BOT_TOKEN)
        ConstructionType.WEBHOOK -> TODO("Implement web hook bot creation")
    }

    fun withCommands(commands: Array<ICommand>): BotBuilder {
        commands.forEach {
            bot.onCommand(it.trigger, it::onCommand)
        }
        return this
    }

    fun withServices(services: Array<IBotService>): BotBuilder {
        services.forEach {
            withCommands(it.getCommands())
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