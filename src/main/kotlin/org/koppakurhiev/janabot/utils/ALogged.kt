package org.koppakurhiev.janabot.utils

import mu.KotlinLogging

abstract class ALogged {
    protected val logger = KotlinLogging.logger(this::class.simpleName ?: "??")
}
