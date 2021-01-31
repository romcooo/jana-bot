package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import com.elbekD.bot.types.MessageEntity
import com.elbekD.bot.types.ReplyKeyboard
import org.koppakurhiev.janabot.features.LivingMessage
import org.koppakurhiev.janabot.services.IBotService


class BotBuilder(constType: ConstructionType = ConstructionType.POLLING) {

    companion object {
        private var BOT_USERNAME = System.getenv("janaBotUsername")
        private var BOT_TOKEN = System.getenv("janaBotToken")
    }

    private val bot: Bot = when (constType) {
        ConstructionType.POLLING -> Bot.createPolling(BOT_USERNAME, BOT_TOKEN)
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
    lifetime: LivingMessage.MessageLifetime = LivingMessage.MessageLifetime.DEFAULT): Any {

    // the extended function is called here, no recursion
    val sentMessage = sendMessage(chatId, text, parseMode, entities, disableWebPagePreview, disableNotification, replyTo, allowSendingWithoutReply, markup)

    val messageCleaner = JanaBot.messageCleaner
    sentMessage.whenComplete { message, _ ->
        messageCleaner.registerMessage(
            LivingMessage(
                chatId = chatId,
                messageId = message.message_id,
                lifetime = lifetime
            )
        )
    }
    return sentMessage
}
