package org.koppakurhiev.janabot.telegram.services.patVpat

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotSubCommand

class Launch(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "launch"

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        standardReply(manager.launch(), conversation) {
            PatVPatStrings.getString(it, "launch")
        }
    }

    override fun help(chat: Chat): String {
        return ""
    }
}

class Stop(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "stop"

    override suspend fun onCommand(message: Message, arguments: String?) {
        val user = message.from?.username
        if (user != null && bot.isBotAdmin(user)) {
            val conversation = Conversation(bot, message)
            standardReply(parent.service.patVPatManager.stop(), conversation) {
                PatVPatStrings.getString(it, "stop.reply")
            }
        }
    }

    override fun help(chat: Chat): String {
        return ""
    }
}

class Broadcast(override val parent: PatVPatCommand) : IBotSubCommand {
    override val command = "broadcast"

    override fun getArguments(): Array<String> {
        return arrayOf("message")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val user = message.from?.username
        if (user != null && bot.isBotAdmin(user)) {
            if (!arguments.isNullOrBlank()) parent.service.patVPatManager.broadcast { arguments }
        }
    }

    override fun help(chat: Chat): String {
        return ""
    }
}