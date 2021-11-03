package org.koppakurhiev.janabot.telegram

import org.jetbrains.annotations.PropertyKey
import org.koppakurhiev.janabot.common.AStringsProvider
import org.koppakurhiev.janabot.common.Strings

object TelegramStrings : AStringsProvider("/TelegramStrings") {
    fun getString(
        locale: Strings.Locale = Strings.Locale.DEFAULT,
        @PropertyKey(resourceBundle = "TelegramStrings") key: String,
        vararg args: Any?
    ): String {
        return getStringsForLocale(locale).get(key, *args)
    }

    fun onMissingArgument(locale: Strings.Locale, arguments: Array<String>, command: String): String {
        val argsText = arguments.joinToString { "<$it>" }
        return getString(locale, "argument.missing", argsText, command)
    }
}