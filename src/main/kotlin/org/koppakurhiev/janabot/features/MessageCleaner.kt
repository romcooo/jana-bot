package org.koppakurhiev.janabot.features

import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.services.ALogged
import java.util.*
import kotlin.concurrent.schedule


class MessageCleaner: ALogged() {

    // not needed atm, but might be handy for retroactive deleting if needed (keep in mind it can only be deleted within 48 hours of posting)
    private val livingMessageList: MutableList<LivingMessage> = mutableListOf()
    private val timer = Timer()

    fun registerMessage(livingMessage: LivingMessage) {
        livingMessageList.add(livingMessage)

        if (livingMessage.lifetime.length > 0) {
            logger.info { "Scheduling message to be deleted: $livingMessage in ${livingMessage.lifetime.length / 1000} seconds." }
            timer.schedule(livingMessage.lifetime.length) {
                livingMessage.kill()
            }
        }
    }

}


data class LivingMessage(val chatId: Any,
                         val messageId: Int,
                         val lifetime: MessageLifetime,
                         val postedDate: Date = Date()) : ALogged() {

    enum class MessageLifetime(private val defaultLength: Long,
                               var length: Long = defaultLength) {
        FOREVER(-1),
        LONG(3600000), // 1 hour
        MEDIUM(600000), // 10 minutes
        SHORT(60000), // minute
        FLASH(10000), // 10sec
        DEFAULT(FLASH.length);

        // You can technically change any of these at runtime by assigning a new value to MessageLifetime.CUSTOM.length
        // This is the reason why there is the private default value - you can use reset() to reset one enum at a time
        // Potentially can be done via a command in the future
        fun reset() {
            this.length = defaultLength
        }

    }

    fun kill() {
        logger.debug { "Deleting message: $this" }
        JanaBot.bot.deleteMessage(chatId, messageId)
    }
}