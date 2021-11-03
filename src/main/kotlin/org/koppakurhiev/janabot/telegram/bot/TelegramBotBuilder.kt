package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.Bot
import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.getLogger
import java.util.*
import kotlin.reflect.KSuspendFunction2

class TelegramBotBuilder(properties: Properties, constType: ConstructionType = ConstructionType.POLLING) {

    private val uiCommands: MutableList<BotCommand> = mutableListOf()

    private val bot: Bot = when (constType) {
        ConstructionType.POLLING -> Bot.createPolling(
            properties.getProperty("bot.username"),
            properties.getProperty("bot.token")
        )
        ConstructionType.WEBHOOK -> TODO("Implement web hook bot creation")
    }

    private fun registerCommands(commands: Array<IBotCommand>) {
        commands.forEach {
            bot.onCommand("/${it.command}", it::onCommand)
            val command = it.getUiCommand()
            if (command != null) {
                uiCommands.add(command)
            }
        }
    }

    fun withServices(services: Set<IBotService>): TelegramBotBuilder {
        services.forEach {
            registerCommands(it.commands)
        }
        return this
    }

    fun onMessage(action: suspend ((message: Message) -> Unit)): TelegramBotBuilder {
        bot.onMessage(action)
        return this
    }

    fun onCallbackQuery(action: KSuspendFunction2<CallbackQuery, String?, Unit>): TelegramBotBuilder {
        bot.onCallbackQuery {
            val data = it.data
            if (data == null) {
                getLogger().warn { "Call back query without data" }
            }
            action.invoke(it, data)
        }
        return this
    }

    fun build(): Bot {
        bot.setMyCommands(uiCommands)
        return bot
    }

    enum class ConstructionType {
        POLLING, WEBHOOK
    }
}