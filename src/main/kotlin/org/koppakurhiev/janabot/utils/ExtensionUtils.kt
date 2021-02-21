package org.koppakurhiev.janabot.utils

import org.koppakurhiev.janabot.JanaBot
import java.time.Duration

/**
 * Gets n-th argument from a list or Null if index is out of range
 */
fun List<String>.getArg(index: Int): String? {
    return if (this.size < index + 1) null else this[index]
}

fun Duration.toFormattedString(): String {
    var seconds = this.seconds
    val days = seconds / 86400
    seconds %= 86400
    val hours = seconds / 3600
    seconds %= 3600
    val minutes = seconds / 60
    seconds %= 60
    return JanaBot.messages.get("timeFormat", days, hours, minutes, seconds)
}