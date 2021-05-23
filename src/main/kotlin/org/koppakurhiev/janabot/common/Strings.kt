package org.koppakurhiev.janabot.common

import com.elbekD.bot.types.Chat
import org.koppakurhiev.janabot.telegram.bot.ChatData
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import java.util.*

open class Strings(bundleAddress: String, locale: Locale) {

    private val messages: Properties

    init {
        val default = Properties()
        var stringsStream = javaClass.getResourceAsStream(bundleAddress + Locale.DEFAULT.localeSuffix + ".properties")
        default.load(stringsStream)
        messages = Properties(default)
        stringsStream = javaClass.getResourceAsStream(bundleAddress + locale.localeSuffix + ".properties")
        messages.load(stringsStream)
        getLogger().info { "${locale.name} locale loaded from $bundleAddress" }
    }

    open fun get(key: String, vararg args: Any?): String {
        val message = messages.getProperty(key)
        return message.format(*args)
    }

    enum class Locale(val localeSuffix: String, val asString: String) {
        SK("_sk", "sk"),
        EN("", "en");

        companion object {
            val DEFAULT = EN
            fun getLocale(localeString: String): Locale {
                return when (localeString) {
                    SK.asString -> SK
                    EN.asString -> EN
                    else -> DEFAULT
                }
            }
        }
    }
}

fun Chat.getLocale(bot: ITelegramBot): Strings.Locale {
    val dataCollection = bot.database.getCollection<ChatData>()
    return dataCollection.findOne(ChatData::chatId eq this.id)?.language ?: Strings.Locale.DEFAULT
}