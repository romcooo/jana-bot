package org.koppakurhiev.janabot.features

import org.jetbrains.annotations.PropertyKey
import org.koppakurhiev.janabot.utils.ALogged
import java.util.*

class StringProvider(localeId: String) : ALogged() {
    private val messages = Properties()

    init {
        val locale = Locales.getLocale(localeId)
        val stringsStream = javaClass.getResourceAsStream(locale.file)
        messages.load(stringsStream)
        logger.info { "${locale.name} locale loaded" }
    }

    fun get(@PropertyKey(resourceBundle = "strings") key: String, vararg args: String): String {
        val message = messages.getProperty(key)
        return message.format(*args)
    }

    enum class Locales(val file: String) {
        SK("/strings_sk.properties"),
        EN("/strings.properties");

        companion object {
            fun getLocale(localeString: String): Locales {
                return when (localeString) {
                    "sk" -> SK
                    "en" -> EN
                    else -> EN //Default
                }
            }
        }
    }
}