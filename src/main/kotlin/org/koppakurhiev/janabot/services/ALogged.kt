package org.koppakurhiev.janabot.services

import mu.KotlinLogging

abstract class ALogged {
    protected val logger = KotlinLogging.logger(this::class.simpleName ?: "??")
}
