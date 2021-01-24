package org.koppakurhiev.janabot.core

import com.elbekD.bot.Bot
import com.elbekD.bot.types.Chat
import org.koppakurhiev.janabot.commands.CustomCommandsForBot

private var BOT_USERNAME = System.getenv("janaBotUsername")
private var BOT_TOKEN = System.getenv("janaBotToken")

class BotBuilder {
    // TODO revisit this and probably enforce usage of .polling() or .webhooking() instead of instantiating immediately
    var bot: Bot = Bot.createPolling(BOT_USERNAME, BOT_TOKEN)

    fun withCommands(): BotBuilder {
        CustomCommandsForBot(bot).addCommands()
        return this
    }

    fun polling(): BotBuilder {
        bot = Bot.createPolling(BOT_USERNAME, BOT_TOKEN)
        return this
    }

    fun build(): Bot {
        return bot
    }

    fun buildDefault(): Bot {
        return polling().withCommands().build()
    }

}

// TODO revisit if this is needed
class BotWrapper(
    val bot: Bot,
    var chatContext: Chat?
) {

    fun messageCurrentChat(text: String): Boolean {
        val safeChat = chatContext
        return if (safeChat?.id != null) {
            bot.sendMessage(safeChat.id, text)
            true
        } else false

    }

}