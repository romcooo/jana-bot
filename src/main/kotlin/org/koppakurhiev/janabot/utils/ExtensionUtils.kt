package org.koppakurhiev.janabot.utils

fun List<String>.getArg(index: Int): String? {
    return if (this.size < index + 1) null else this[index]
}