package org.koppakurhiev.janabot.common

import mu.KLogger
import mu.KotlinLogging

/**
 * Gets n-th argument from a list or Null if index is out of range
 */
fun List<String>.getArg(index: Int): String? {
    return if (this.size < index + 1) null else this[index]
}

/**
 * Gets all elements after n-th joined by given separator as single string
 */
fun List<String>.after(index: Int, separator: CharSequence): String? {
    if (this.size < index) return null
    val theRest = this.drop(index + 1)
    return theRest.joinToString(separator)
}

/**
 * Returns logger for this class
 */
fun Any.getLogger(): KLogger {
    return KotlinLogging.logger(this::class.simpleName ?: "??")
}