package org.koppakurhiev.janabot.telegram

import org.jetbrains.annotations.PropertyKey
import org.koppakurhiev.janabot.common.Strings

object TelegramStrings {
    private val skLocale = Strings("/telegramStrings", Strings.Locale.SK)
    private val enLocale = Strings("/telegramStrings", Strings.Locale.EN)

    fun getString(
        locale: Strings.Locale,
        @PropertyKey(resourceBundle = "telegramStrings") key: String,
        vararg args: Any?
    ): String {
        return getStringsForLocale(locale).get(key, *args)
    }

    fun getString(@PropertyKey(resourceBundle = "telegramStrings") key: String, vararg args: Any?): String {
        return getStringsForLocale(Strings.Locale.DEFAULT).get(key, *args)
    }

    private fun getStringsForLocale(locale: Strings.Locale): Strings {
        return when (locale) {
            Strings.Locale.SK -> skLocale
            Strings.Locale.EN -> enLocale
        }
    }
}