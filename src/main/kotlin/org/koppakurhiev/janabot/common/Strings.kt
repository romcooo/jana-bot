package org.koppakurhiev.janabot.common

import java.util.*

open class Strings(bundleAddress: String, locale: Locale) : ALogged() {

    private val messages = Properties()

    init {
        val stringsStream = javaClass.getResourceAsStream(bundleAddress + locale.localeSuffix + ".properties")
        messages.load(stringsStream)
        logger.info { "${locale.name} locale loaded from $bundleAddress" }
    }

    open fun get(key: String, vararg args: Any?): String {
        val message = messages.getProperty(key)
        return message.format(*args)
    }

    enum class Locale(val localeSuffix: String) {
        SK("_sk"),
        EN("");

        companion object {
            val DEFAULT = EN
            fun getLocale(localeString: String): Locale {
                return when (localeString) {
                    "sk" -> SK
                    "en" -> EN
                    else -> DEFAULT
                }
            }
        }
    }
}