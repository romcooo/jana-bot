package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.CommonStrings
import org.koppakurhiev.janabot.common.getArg
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings

interface IBotCommand {
    val service: IBotService
    val command: String
    val bot: ITelegramBot
        get() = service.bot

    fun initialize() {}
    fun deconstruct() {}

    suspend fun onCommand(message: Message, arguments: String?)
    suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean
    fun getUiCommand(): BotCommand?
    fun help(chat: Chat): String
    fun getArguments(): Array<String>

    fun defaultHelp(helpText: String): String {
        val stringBuilder = StringBuilder("*$command*")
        getArguments().forEach {
            stringBuilder.append(" <$it>")
        }
        stringBuilder.append(" - $helpText")
        return stringBuilder.toString()
    }
}

abstract class ABotCommandWithSubCommands : IBotCommand {

    abstract val subCommands: Set<IBotSubCommand>

    abstract suspend fun onNoArguments(message: Message, argument: String?)

    open fun plainHelp(chat: Chat): String? {
        return null
    }

    override fun initialize() {
        subCommands.forEach {
            it.initialize()
        }
    }

    override fun deconstruct() {
        subCommands.forEach {
            it.deconstruct()
        }
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        if (arguments == null || !arguments.startsWith("-")) {
            onNoArguments(message, arguments)
        } else {
            val args = arguments.split(" ")
            val subCommand = args.getArg(0)
            if (subCommand == "-help") {
                val conversation = Conversation(bot, message)
                conversation.replyMessage(help(message.chat))
            } else {
                subCommands.forEach {
                    if (subCommand == "-${it.command}") {
                        val values = arguments.replace(subCommand, "").trim()
                        it.onCommand(message, values)
                        return
                    }
                }
                //No command executed
                val conversation = Conversation(bot, message)
                conversation.replyMessage(CommonStrings.getString(conversation.language, "unknownCommand", arguments))
            }
        }
    }

    override fun help(chat: Chat): String {
        val builder = StringBuilder(TelegramStrings.getString(chat.getLocale(bot), "help.aggregated", command))
        val noSubCommandHelp = plainHelp(chat)
        if (!noSubCommandHelp.isNullOrBlank()) {
            builder.appendLine()
            val text = TelegramStrings.getString(chat.getLocale(bot), "noSubCommand.help")
            builder.append("    $text - $noSubCommandHelp")
        }
        subCommands.forEach { subCommand ->
            val helpText = subCommand.help(chat)
            if (helpText.isNotBlank()) {
                builder.appendLine()
                builder.append("    -$helpText")
            }
        }
        return builder.toString()
    }
}