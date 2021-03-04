package org.koppakurhiev.janabot.features

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.utils.ALogged
import java.util.*
import kotlin.concurrent.schedule

object MessageCleaner : ALogged() {
    private val timer = Timer()

    fun registerMessage(message: Message, lifetime: MessageLifetime) {
        if (lifetime != MessageLifetime.FOREVER) {
            logger.trace { "Scheduling message to be deleted: $message in ${lifetime.length / 1000} seconds." }
            timer.schedule(lifetime.length) {
                logger.trace { "Deleting message: $message" }
                JanaBot.bot.deleteMessage(message.chat.id, message.message_id)
            }
        }
    }
}

enum class MessageLifetime(val length: Long) {
    /**
     * The message will not be deleted
     */
    FOREVER(-1L),

    /**
     * 1 hour
     */
    LONG(3600000L),

    /**
     * 10 minutes
     */
    MEDIUM(600000L),

    /**
     * 2 minute
     */
    SHORT(120000L),

    /**
     * 30 seconds
     */
    FLASH(30000L),

    /**
     * Default message lifetime, currently [FOREVER]
     */
    DEFAULT(FOREVER.length);
}