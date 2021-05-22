package org.koppakurhiev.janabot.telegram.services.timer

import org.litote.kmongo.Id
import org.litote.kmongo.newId

data class TimerReaction @JvmOverloads constructor(
    val _id: Id<TimerReaction> = newId(),
    val type: Type = Type.FINE,
    val text: String = String()
) {
    enum class Type {
        RECORD,
        LAME,
        FINE,
        COOL
    }
}
