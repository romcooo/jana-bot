package org.koppakurhiev.janabot.services

import mu.KotlinLogging

abstract class ABotService : IBotService {
    protected val logger = KotlinLogging.logger {}

    abstract class ACommand(override val trigger: String) : IBotService.ICommand {
        protected val logger = KotlinLogging.logger {}
    }
}