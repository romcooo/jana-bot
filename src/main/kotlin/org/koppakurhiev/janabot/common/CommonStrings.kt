package org.koppakurhiev.janabot.common

import org.jetbrains.annotations.PropertyKey

object CommonStrings {
    private val skLocale = Strings("/strings", Strings.Locale.SK)
    private val enLocale = Strings("/strings", Strings.Locale.EN)

    fun getString(
        locale: Strings.Locale,
        @PropertyKey(resourceBundle = "strings") key: String,
        vararg args: Any?
    ): String {
        return getStringsForLocale(locale).get(key, *args)
    }

    fun getString(@PropertyKey(resourceBundle = "strings") key: String, vararg args: Any?): String {
        return getStringsForLocale(Strings.Locale.DEFAULT).get(key, args)
    }

    private fun getStringsForLocale(locale: Strings.Locale): Strings {
        return when (locale) {
            Strings.Locale.SK -> skLocale
            Strings.Locale.EN -> enLocale
        }
    }
}