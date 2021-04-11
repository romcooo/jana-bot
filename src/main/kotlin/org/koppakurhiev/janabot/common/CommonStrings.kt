package org.koppakurhiev.janabot.common

import org.jetbrains.annotations.PropertyKey

object CommonStrings : AStringsProvider("/CommonStrings") {
    fun getString(
        locale: Strings.Locale = Strings.Locale.DEFAULT,
        @PropertyKey(resourceBundle = "CommonStrings") key: String,
        vararg args: Any?
    ): String {
        return getStringsForLocale(locale).get(key, *args)
    }
}