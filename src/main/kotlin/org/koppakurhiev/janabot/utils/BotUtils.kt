package org.koppakurhiev.janabot.utils

import com.elbekD.bot.Bot
import com.elbekD.bot.types.MessageEntity
import com.elbekD.bot.types.ReplyKeyboard
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.MessageCleaner
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.IBotService
import java.util.*


class BotBuilder(properties: Properties, constType: ConstructionType = ConstructionType.POLLING) {

    private val bot: Bot = when (constType) {
        ConstructionType.POLLING -> Bot.createPolling(
            properties.getProperty("bot.username"),
            properties.getProperty("bot.token")
        )
        ConstructionType.WEBHOOK -> TODO("Implement web hook bot creation")
    }

    private fun registerCommands(commands: Array<IBotService.ICommand>) {
        commands.forEach {
            bot.onCommand(it.trigger, it::onCommand)
        }
    }

    fun withServices(services: Array<IBotService>): BotBuilder {
        services.forEach {
            registerCommands(it.getCommands())
            bot.onMessage(it::onMessage)
        }
        return this
    }

    fun build(): Bot {
        return bot
    }

    enum class ConstructionType {
        POLLING, WEBHOOK
    }
}

// extension function to allow deleting of messages after a period of time
fun Bot.sendMessage(
    chatId: Any,
    text: String,
    parseMode: String? = null,
    entities: List<MessageEntity>? = null,
    disableWebPagePreview: Boolean? = null,
    disableNotification: Boolean? = null,
    replyTo: Int? = null,
    allowSendingWithoutReply: Boolean? = null,
    markup: ReplyKeyboard? = null,
    lifetime: MessageLifetime = MessageLifetime.DEFAULT
): Any {

    // the extended function is called here, no recursion
    val sentMessage = sendMessage(
        chatId,
        text,
        parseMode,
        entities,
        disableWebPagePreview,
        disableNotification,
        replyTo,
        allowSendingWithoutReply,
        markup
    )

    sentMessage.whenComplete { message, _ ->
        MessageCleaner.registerMessage(message, lifetime)
    }
    return sentMessage
}

class SimpleConversationContext(
    val chatId: Any,
    val triggerMessageId: Int?
) {
    fun sendMessage(text: String, lifetime: MessageLifetime? = null) {
        JanaBot.bot.sendMessage(
            chatId = chatId,
            text = text,
            replyTo = triggerMessageId,
            lifetime = lifetime ?: MessageLifetime.DEFAULT
        )
    }
}
