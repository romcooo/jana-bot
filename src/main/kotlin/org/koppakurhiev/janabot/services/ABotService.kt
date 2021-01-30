package org.koppakurhiev.janabot.services

abstract class ABotService : ALogged(), IBotService {
    abstract class ACommand(override val trigger: String) : ALogged(), IBotService.ICommand
}
