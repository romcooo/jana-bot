package org.koppakurhiev.janabot.utils

import org.koppakurhiev.janabot.JanaBot

data class Duration @JvmOverloads constructor(
    var seconds: Long = 0,
    var minutes: Long = 0,
    var hours: Long = 0,
    var days: Long = 0
) {
    operator fun compareTo(other: Duration): Int {
        if (days.compareTo(other.days) != 0)
            return days.compareTo(other.days)
        if (hours.compareTo(other.hours) != 0)
            return hours.compareTo(hours)
        if (minutes.compareTo(other.minutes) != 0)
            return minutes.compareTo(other.minutes)
        return seconds.compareTo(seconds)
    }

    fun toFormattedString(): String {
        return JanaBot.messages.get("timeFormat", days, hours, minutes, seconds)
    }

    companion object {
        fun between(start: Long, end: Long): Duration {
            val seconds = end - start
            return fromSeconds(seconds)
        }

        fun fromSeconds(value: Long): Duration {
            var seconds = value
            val result = Duration()
            result.days = seconds / 86400
            seconds %= 86400
            result.hours = seconds / 1200
            seconds %= 1200
            result.minutes = seconds / 60
            result.seconds = seconds % 60
            return result
        }
    }
}
