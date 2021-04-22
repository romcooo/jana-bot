package org.koppakurhiev.janabot.telegram.services

import com.elbekD.bot.types.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koppakurhiev.janabot.common.ALogged
import org.koppakurhiev.janabot.common.CommonStrings
import org.koppakurhiev.janabot.common.Strings
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.*
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOne

class DefaultCommandsService(override val bot: ITelegramBot) : IBotService {

    override val commands: Array<IBotCommand> = arrayOf(
        Help(this),
        Start(this),
        Invite(this),
        Language(this),
        Broadcast(this),
    )

    override suspend fun onMessage(message: Message) {
        //Nothing
    }

    private class Help(override val service: IBotService) : IBotCommand, ALogged() {
        override val command = "help"

        override fun getUiCommand(): BotCommand {
            return BotCommand(command, "Prints the help text with all information.")
        }

        override fun getArguments(): Array<String> {
            return emptyArray()
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            val messageBuilder = StringBuilder(TelegramStrings.getString(conversation.language, key = "help.beginning"))
            bot.services.forEach {
                val line = it.help(message.chat)
                if (line.isNotBlank()) {
                    messageBuilder.appendLine(line)
                }
            }
            conversation.parseMode = Conversation.ParseMode.MARKDOWN
            conversation.replyMessage(messageBuilder.toString())
            //TODO swap for close button
            conversation.burnConversation(MessageLifetime.MEDIUM)
            logger.trace { "/help command executed in channel " + message.chat.id }
        }

        override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
            return false //TODO -later
        }

        override fun help(chat: Chat): String {
            return defaultHelp(TelegramStrings.getString(chat.getLocale(bot), "help.help"))
        }
    }

    private class Start(override val service: IBotService) : IBotCommand, ALogged() {
        override val command = "start"

        override fun getUiCommand(): BotCommand {
            return BotCommand(command, "Gets the bot to talk to you.")
        }

        override fun getArguments(): Array<String> {
            return emptyArray()
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            val username = message.from?.username ?: CommonStrings.getString(conversation.language, "person")
            conversation.replyMessage(TelegramStrings.getString(conversation.language, "start.text", username))
            logger.debug { "/start command executed in channel " + message.chat.id }
        }

        override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
            return false
        }

        override fun help(chat: Chat): String {
            return defaultHelp(TelegramStrings.getString(chat.getLocale(bot), "start.help"))
        }
    }

    private class Invite(override val service: IBotService) : IBotCommand {
        override val command = "invite"

        override fun getUiCommand(): BotCommand {
            return BotCommand(command, "Gets you the link to the bots private chat.")
        }

        override fun getArguments(): Array<String> {
            return emptyArray()
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            conversation.replyMessage(
                TelegramStrings.getString(conversation.language, "invite.text", bot.name)
            )
        }

        override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
            return false
        }

        override fun help(chat: Chat): String {
            return defaultHelp(TelegramStrings.getString(chat.getLocale(bot), "invite.help"))
        }
    }

    private class Language(override val service: IBotService) : IBotCommand, ALogged() {
        override val command = "language"

        override fun getUiCommand(): BotCommand {
            return BotCommand(command, "Set primary communication language")
        }

        override fun getArguments(): Array<String> {
            return arrayOf("language")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            if (arguments.isNullOrEmpty()) {
                val buttons = mutableListOf<InlineKeyboardButton>()
                //TODO adjust data
                buttons.add(InlineKeyboardButton(text = "English", callback_data = "$command en"))
                buttons.add(InlineKeyboardButton(text = "Slovensky", callback_data = "$command sk"))
                val markup = InlineKeyboardMarkup(listOf(buttons))
                bot.telegramBot.sendMessage(
                    chatId = message.chat.id,
                    text = TelegramStrings.getString(message.chat.getLocale(bot), "language.get"),
                    markup = markup
                )
            } else {
                val conversation = Conversation(bot, message)
                val locale = Strings.Locale.getLocale(arguments)
                setLanguage(message.chat.id, locale)
                conversation.replyMessage(TelegramStrings.getString(locale, "language.set"))
            }
        }

        override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
            val locale = Strings.Locale.getLocale(arguments)
            setLanguage(query.message!!.chat.id, locale)
            bot.telegramBot.answerCallbackQuery(query.id, TelegramStrings.getString(locale, "language.set"))
            return true
        }

        private fun setLanguage(chatId: Long, locale: Strings.Locale) {
            val collection = bot.database.getCollection<ChatData>()
            val data = collection.findOne(ChatData::chatId eq chatId)
            if (data == null) {
                logger.error { "Set language called but not ChatData found" }
            } else {
                data.language = locale
                collection.updateOne(data)
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(TelegramStrings.getString(chat.getLocale(bot), "language.help"))
        }

    }

    private class Broadcast(override val service: IBotService) : IBotCommand {
        override val command = "broadcast"

        override suspend fun onCommand(message: Message, arguments: String?) {
            val user = message.from
            if (bot.isBotAdmin(user?.username)) {
                val chatsCollection = bot.database.getCollection<ChatData>().find()
                chatsCollection.forEach {
                    val conversation = Conversation(bot, it.chatId)
                    if (arguments != null) {
                        GlobalScope.launch {
                            conversation.sendMessage(arguments)
                        }
                    }
                }
            }
        }

        override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
            return false
        }

        override fun getUiCommand(): BotCommand? {
            return null
        }

        override fun help(chat: Chat): String {
            return ""
        }

        override fun getArguments(): Array<String> {
            return arrayOf("message")
        }
    }
}
