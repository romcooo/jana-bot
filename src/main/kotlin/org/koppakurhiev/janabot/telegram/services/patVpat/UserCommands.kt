package org.koppakurhiev.janabot.telegram.services.patVpat

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotSubCommand
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime

class Subscribe(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "subscribe"

    override suspend fun onCommand(message: Message, arguments: String?) {
        val username = "${message.from?.first_name} ${message.from?.last_name}"
        val conversation = Conversation(bot, message)
        val result = manager.subscribe(message.chat, username)
        standardReply(result, conversation) {
            PatVPatStrings.getString(it, "subscribe")
        }
        if (result == PatVPatManager.OperationResult.SUCCESS) {
            conversation.sendMessage(PatVPatStrings.getString(conversation.language, "rules"))
            if (manager.isQuestionAsked()) {
                manager.askUser(conversation)
            } else {
                conversation.sendMessage(PatVPatStrings.getString(conversation.language, "noQuestion"))
            }
        }
    }

    override fun help(chat: Chat): String {
        return if (manager.isSubscribed(chat.id)) {
            ""
        } else {
            return defaultHelp(PatVPatStrings.getString(chat.getLocale(bot), "subscribe.help"))
        }
    }
}

class Unsubscribe(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "unsubscribe"

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        standardReply(manager.unsubscribe(message.chat), conversation) {
            PatVPatStrings.getString(it, "unsubscribe")
        }
    }

    override fun help(chat: Chat): String {
        return if (manager.isSubscribed(chat.id)) {
            defaultHelp(PatVPatStrings.getString(chat.getLocale(bot), "unsubscribe.help"))
        } else {
            ""
        }
    }
}

class Reminders(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "reminders"

    override fun getArguments(): Array<String> {
        return arrayOf("on/off")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        when {
            arguments.isNullOrBlank() -> {
                conversation.replyMessage(
                    TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                )
                conversation.burnConversation(MessageLifetime.FLASH)
            }
            arguments == "on" -> standardReply(manager.remindersOn(message.chat), conversation) {
                PatVPatStrings.getString(it, "reminders.on")
            }
            arguments == "off" -> standardReply(manager.remindersOff(message.chat), conversation) {
                PatVPatStrings.getString(it, "reminders.off")
            }
            else -> {
                conversation.replyMessage(TelegramStrings.getString(conversation.language, "argument.unknown"))
                conversation.burnConversation(MessageLifetime.FLASH)
            }
        }
    }

    override fun help(chat: Chat): String {
        return if (manager.isSubscribed(chat.id)) {
            defaultHelp(PatVPatStrings.getString(chat.getLocale(bot), "reminders.help"))
        } else {
            ""
        }
    }
}

class AddQuestion(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "addQ"

    override fun getArguments(): Array<String> {
        return arrayOf("text")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val username = "${message.from?.first_name} ${message.from?.last_name}"
        val conversation = Conversation(bot, message)
        if (arguments.isNullOrBlank()) {
            conversation.replyMessage(
                TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
            )
            conversation.burnConversation(MessageLifetime.FLASH)
        } else {
            standardReply(manager.addQuestion(arguments, username), conversation) {
                PatVPatStrings.getString(it, "addQ", arguments, manager.getQuestionPoolSize())
            }
        }
    }

    override fun help(chat: Chat): String {
        return defaultHelp(PatVPatStrings.getString(chat.getLocale(bot), "addQ.help"))
    }
}

class AddAnswer(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "addA"

    override fun getArguments(): Array<String> {
        return arrayOf("text")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        if (arguments.isNullOrBlank()) {
            conversation.replyMessage(
                TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
            )
            conversation.burnConversation(MessageLifetime.FLASH)
        } else {
            standardReply(manager.addAnswer(message.chat, arguments), conversation) {
                PatVPatStrings.getString(it, "addA", arguments)
            }
        }
    }

    override fun help(chat: Chat): String {
        return if (manager.isSubscribed(chat.id)) {
            defaultHelp(PatVPatStrings.getString(chat.getLocale(bot), "addA.help"))
        } else {
            ""
        }
    }
}

class CatchUp(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "catchUp"

    override suspend fun onCommand(message: Message, arguments: String?) {
        TODO("Not yet implemented")
    }

    override fun help(chat: Chat): String {
        return "" //TODO
    }
}

class Skip(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "skip"

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        standardReply(manager.skipQuestion(message.chat), conversation) { "" }
    }

    override fun help(chat: Chat): String {
        return if (manager.isSubscribed(chat.id)) {
            defaultHelp(PatVPatStrings.getString(chat.getLocale(bot), "skip.help"))
        } else {
            ""
        }
    }
}

class Stats(override val parent: PatVPatCommand) : IBotSubCommand {
    override val command = "stats"

    override fun getArguments(): Array<String> {
        return emptyArray()
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val manager = parent.service.patVPatManager
        val conversation = Conversation(bot, message)
        val allQuestionCount = manager.getQuestionPoolSize()
        val askedCount = manager.getAskedQuestionsCount()
        val skippedCount = manager.getSkippedQuestionsCount()
        val subscribers = manager.getSubscribersCount()
        conversation.replyMessage(
            PatVPatStrings.getString(
                conversation.language,
                "stats",
                allQuestionCount,
                askedCount,
                skippedCount,
                allQuestionCount - askedCount,
                subscribers
            )
        )
    }

    override fun help(chat: Chat): String {
        return defaultHelp(PatVPatStrings.getString(chat.getLocale(bot), "stats.help"))
    }
}

class Rules(override val parent: PatVPatCommand) : IBotSubCommand {
    override val command = "rules"

    override fun getArguments(): Array<String> {
        return emptyArray()
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val conversation = Conversation(bot, message)
        conversation.sendMessage(PatVPatStrings.getString(conversation.language, "rules"))
    }

    override fun help(chat: Chat): String {
        return defaultHelp(PatVPatStrings.getString(chat.getLocale(bot), "rules.help"))
    }
}