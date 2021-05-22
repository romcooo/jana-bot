package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.Bot
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.Message
import com.mongodb.client.MongoDatabase
import org.koppakurhiev.janabot.common.getArg
import org.koppakurhiev.janabot.common.getLogger
import org.koppakurhiev.janabot.persistence.MongoRepository
import org.koppakurhiev.janabot.telegram.TelegramStrings
import java.util.*

abstract class ATelegramBot(final override val resourceFolder: String) : ITelegramBot {
    final override val properties: Properties
    override val services: MutableSet<IBotService> = mutableSetOf()
    override lateinit var telegramBot: Bot

    override val name: String get() = properties.getProperty("bot.username")
    override val database: MongoDatabase get() = repository.database

    private val repository: MongoRepository

    init {
        val configStream = javaClass.getResourceAsStream("/$resourceFolder/config.properties")
        properties = Properties()
        properties.load(configStream)
        configStream?.close()
        repository = MongoRepository(properties)
    }

    private suspend fun onMessage(message: Message) {
        services.forEach {
            it.onMessage(message)
        }
    }

    override fun isBotAdmin(username: String?): Boolean {
        if (username == null) return false
        val admins = properties.getProperty("admins").split(", ")
        return admins.contains(username)
    }

    protected suspend fun deconstructServices() {
        services.forEach {
            it.deconstruct()
        }
    }

    protected suspend fun initializeServices() {
        services.forEach {
            it.initialize()
        }
    }

    fun buildBot() {
        telegramBot = TelegramBotBuilder(properties)
            .withServices(services)
            .onMessage(this::onMessage)
            .onCallbackQuery(this::onCallbackQuery)
            .build()
        telegramBot.start()
    }

    override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String?) {
        getLogger().trace { "Callback with arguments '$arguments' \n $query" }
        var isResolved = false
        if (arguments == null) {
            onCallbackQueryWithNoData(query)
        } else {
            val command = arguments.split(" ")
            services.forEach { service ->
                service.commands.forEach {
                    if (it.command == command.getArg(0)) {
                        isResolved = it.onCallbackQuery(query, arguments.removePrefix(command[0]).trim())
                    }
                }
            }
            if (!isResolved) {
                telegramBot.answerCallbackQuery(query.id, TelegramStrings.getString(key = "callback.unresolved"))
            }
        }
    }

    abstract fun onCallbackQueryWithNoData(query: CallbackQuery)
}