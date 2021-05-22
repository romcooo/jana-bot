package org.koppakurhiev.janabot.common

data class Duration constructor(
    val seconds: Long
) {
    operator fun compareTo(other: Duration): Int {
        return seconds.compareTo(other.seconds)
    }

    fun toFormattedString(locale: Strings.Locale = Strings.Locale.DEFAULT): String {
        var locSeconds = seconds
        val days = locSeconds / SECONDS_IN_DAY
        locSeconds %= SECONDS_IN_DAY
        val hours = locSeconds / SECONDS_IN_HOUR
        locSeconds %= SECONDS_IN_HOUR
        val minutes = locSeconds / SECONDS_IN_MINUTE
        locSeconds %= SECONDS_IN_MINUTE
        return CommonStrings.getString(locale, "timeFormat", days, hours, minutes, locSeconds)
    }

    fun getYears(): Long {
        return seconds / SECONDS_IN_YEAR
    }

    fun getDays(): Long {
        return seconds / SECONDS_IN_DAY
    }

    fun getHours(): Long {
        return seconds / SECONDS_IN_HOUR
    }

    fun getMinutes(): Long {
        return seconds / SECONDS_IN_MINUTE
    }

    companion object {

        const val SECONDS_IN_YEAR = 31536000L
        const val SECONDS_IN_DAY = 86400L
        const val SECONDS_IN_HOUR = 3600L
        const val SECONDS_IN_MINUTE = 60L

        fun between(start: Long, end: Long): Duration {
            return Duration(end - start)
        }
    }
}
