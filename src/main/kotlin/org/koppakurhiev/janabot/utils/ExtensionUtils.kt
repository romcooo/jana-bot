package org.koppakurhiev.janabot.utils

/**
 * Gets n-th argument from a list or Null if index is out of range
 */
fun List<String>.getArg(index: Int): String? {
    return if (this.size < index + 1) null else this[index]
}