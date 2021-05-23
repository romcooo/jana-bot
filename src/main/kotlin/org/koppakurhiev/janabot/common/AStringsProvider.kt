package org.koppakurhiev.janabot.common

abstract class AStringsProvider(private val stringsPackage: String) {

    private val skLocale = Strings(stringsPackage, Strings.Locale.SK)
    private val enLocale = Strings(stringsPackage, Strings.Locale.EN)

    protected fun getStringsForLocale(locale: Strings.Locale): Strings {
        return when (locale) {
            Strings.Locale.SK -> skLocale
            Strings.Locale.EN -> enLocale
        }
    }
}