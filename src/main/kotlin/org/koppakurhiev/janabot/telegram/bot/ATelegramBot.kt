package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.Bot
import com.elbekD.bot.types.Message
import com.mongodb.client.MongoDatabase
import org.koppakurhiev.janabot.common.ALogged
import org.koppakurhiev.janabot.persistence.MongoRepository
import java.util.*

abstract class ATelegramBot(private val resourceFolder: String) : ALogged(), ITelegramBot {
    private lateinit var bot: Bot

    private val properties: Properties
    private val repository: MongoRepository
    override val services: MutableSet<IBotService> = mutableSetOf()

    init {
        val configStream = javaClass.getResourceAsStream("/$resourceFolder/config.properties")
        properties = Properties()
        properties.load(configStream)
        configStream.close()
        repository = MongoRepository(properties)
    }

    override fun getResourceFolder(): String {
        return resourceFolder
    }

    override fun getName(): String {
        return properties.getProperty("bot.username")
    }

    override fun getBot(): Bot {
        return bot
    }

    override fun getDatabase(): MongoDatabase {
        return repository.database
    }

    override fun getProperties(): Properties {
        return properties
    }

    private suspend fun onMessage(message: Message) {
        services.forEach {
            it.onMessage(message)
        }
    }

    override fun isBotAdmin(username: String): Boolean {
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
        bot = TelegramBotBuilder(getProperties())
            .withServices(services)
            .onMessage(this::onMessage)
            .build()
        bot.start()
    }
}