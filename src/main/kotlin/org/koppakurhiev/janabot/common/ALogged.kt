package org.koppakurhiev.janabot.common

import mu.KotlinLogging

abstract class ALogged {
    protected val logger = KotlinLogging.logger(this::class.simpleName ?: "??")
}
