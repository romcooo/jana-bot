package org.koppakurhiev.janabot.services

import org.koppakurhiev.janabot.utils.ALogged

abstract class ABotService : ALogged(), IBotService {
    abstract class ACommand(override val trigger: String) : ALogged(), IBotService.ICommand
}
