package org.koppakurhiev.janabot.telegram.services.timer

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.CommonStrings
import org.koppakurhiev.janabot.common.Duration
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class TimerService(override val bot: ITelegramBot) : IBotService {

    val timerManager = TimerManager(bot)

    override val commands = arrayOf<IBotCommand>(
        TimerCommand(this)
    )

    override suspend fun onMessage(message: Message) {
        //Nothing
    }

    private class TimerCommand(override val service: TimerService) : ABotCommandWithSubCommands() {
        override val command = "timer"

        override val subCommands = setOf(
            Info(this),
            Record(this),
            AddReaction(this)
        )

        val manager get() = service.timerManager

        override fun getUiCommand(): BotCommand {
            return BotCommand(command, "Resets THE timer")
        }

        override fun getArguments(): Array<String> {
            return emptyArray()
        }

        fun shouldTalk(): Boolean {
            return Random.nextFloat() > 0.25
        }

        fun getResponseType(duration: Duration): TimerReaction.Type {
            return when {
                duration.seconds < 2 * Duration.SECONDS_IN_HOUR -> {
                    TimerReaction.Type.LAME
                }
                duration.seconds < 3 * Duration.SECONDS_IN_DAY -> {
                    TimerReaction.Type.FINE
                }
                else -> {
                    TimerReaction.Type.COOL
                }
            }
        }

        override suspend fun onNoArguments(message: Message, argument: String?) {
            val conversation = Conversation(bot, message)
            val oldRecord = manager.getData(conversation.chatId)?.record
            when (manager.resetTimer(conversation.chatId)) {
                TimerManager.OperationResult.INITIATED -> {
                    conversation.replyMessage(
                        TelegramStrings.getString(conversation.language, "timer.initiated")
                    )
                }
                TimerManager.OperationResult.SUCCESS -> {
                    val runLength = manager.getData(conversation.chatId)?.lastRunLength
                    if (runLength != null) {
                        conversation.replyMessage(
                            TelegramStrings.getString(
                                conversation.language,
                                "timer.reset",
                                runLength.toFormattedString(conversation.language)
                            )
                        )
                        if (shouldTalk()) {
                            val reaction = manager.getReaction(getResponseType(runLength))
                            if (reaction != null) {
                                conversation.sendMessage(reaction)
                            }
                        }
                    }
                }
                TimerManager.OperationResult.RECORD_BROKEN -> {
                    conversation.replyMessage(
                        TelegramStrings.getString(
                            conversation.language,
                            "timer.reset",
                            manager.getData(conversation.chatId)?.lastRunLength?.toFormattedString(conversation.language)
                        )
                    )
                    conversation.sendMessage(
                        TelegramStrings.getString(
                            conversation.language,
                            "timer.recordBroken",
                            oldRecord?.toFormattedString(conversation.language)
                        )
                    )
                    if (shouldTalk()) {
                        val reaction = manager.getReaction(TimerReaction.Type.RECORD)
                        if (reaction != null) {
                            conversation.sendMessage(reaction)
                        }
                    }
                }
                TimerManager.OperationResult.SAVE_FAILED -> {
                    conversation.replyMessage(CommonStrings.getString(conversation.language, "db.saveFailed"))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
            }
        }

        override fun plainHelp(chat: Chat): String {
            return TelegramStrings.getString(chat.getLocale(bot), "timer.help")
        }

        override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
            return false
        }

        private class Info(override val parent: TimerCommand) : IBotSubCommand {
            override val command = "info"

            override fun getArguments(): Array<String> {
                return emptyArray()
            }

            override suspend fun onCommand(message: Message, arguments: String?) {
                val conversation = Conversation(bot, message)
                val start = parent.manager.getData(conversation.chatId)?.timer
                if (start != null) {
                    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    val since = formatter.format(start)
                    val runningFor = Duration.between(
                        start.toEpochSecond(ZoneOffset.UTC),
                        LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                    )
                    conversation.replyMessage(
                        TelegramStrings.getString(
                            conversation.language,
                            "timer.status",
                            since,
                            runningFor.toFormattedString(conversation.language)
                        )
                    )
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
            }

            override fun help(chat: Chat): String {
                return defaultHelp(TelegramStrings.getString(chat.getLocale(bot), "timer.info.help"))
            }
        }

        private class Record(override val parent: TimerCommand) : IBotSubCommand {
            override val command = "record"

            override fun getArguments(): Array<String> {
                return emptyArray()
            }

            override suspend fun onCommand(message: Message, arguments: String?) {
                val conversation = Conversation(bot, message)
                val record = parent.manager.getData(conversation.chatId)?.record
                val text = if (record != null) {
                    TelegramStrings.getString(
                        conversation.language,
                        "timer.recordInfo",
                        record.toFormattedString(conversation.language)
                    )
                } else {
                    TelegramStrings.getString(conversation.language, "timer.noRecord")
                }
                conversation.replyMessage(text)
                conversation.burnConversation(MessageLifetime.MEDIUM)
            }

            override fun help(chat: Chat): String {
                return defaultHelp(TelegramStrings.getString(chat.getLocale(bot), "timer.record.help"))
            }
        }

        private class AddReaction(override val parent: TimerCommand) : IBotSubCommand {
            override val command = "addReaction"

            override fun getArguments(): Array<String> {
                return arrayOf("type", "text")
            }

            override fun help(chat: Chat): String {
                return ""
            }

            override suspend fun onCommand(message: Message, arguments: String?) {
                if (bot.isBotAdmin(message.from?.username)) {
                    val conversation = Conversation(bot, message)
                    val typeText = arguments?.split(" ")?.get(0)
                    val text = arguments?.dropWhile { it != ' ' }
                    val type = typeText?.trim()?.toUpperCase()?.let { TimerReaction.Type.valueOf(it) }
                    if (type == null || text.isNullOrBlank()) {
                        conversation.replyMessage(
                            TelegramStrings.onMissingArgument(
                                conversation.language,
                                getArguments(),
                                command
                            )
                        )
                    } else {
                        if (parent.manager.addReaction(type, text) != TimerManager.OperationResult.SUCCESS) {
                            conversation.replyMessage(CommonStrings.getString(conversation.language, "db.saveFailed"))
                        }
                    }
                }
            }
        }
    }
}