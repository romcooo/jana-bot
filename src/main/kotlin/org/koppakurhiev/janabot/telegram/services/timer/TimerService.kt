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
        )

        val manager get() = service.timerManager
        var lastReset: Conversation? = null

        override fun getUiCommand(): BotCommand {
            return BotCommand(command, "Resets THE timer")
        }

        override fun getArguments(): Array<String> {
            return emptyArray()
        }

        override suspend fun onNoArguments(message: Message, argument: String?) {
            val conversation = Conversation(bot, message)
            val oldRecord = manager.timerData.record
            when (manager.resetTimer()) {
                TimerManager.OperationResult.SUCCESS -> {
                    conversation.replyMessage(
                        TelegramStrings.getString(
                            conversation.language,
                            "timer.reset",
                            manager.timerData.lastRunLength.toFormattedString(conversation.language)
                        )
                    )
                    lastReset?.burnConversation(MessageLifetime.FLASH)
                    lastReset = conversation
                }
                TimerManager.OperationResult.RECORD_BROKEN -> {
                    conversation.replyMessage(
                        TelegramStrings.getString(
                            conversation.language,
                            "timer.reset",
                            manager.timerData.lastRunLength.toFormattedString(conversation.language)
                        )
                    )
                    conversation.sendMessage(
                        TelegramStrings.getString(
                            conversation.language,
                            "timer.recordBroken",
                            oldRecord.toFormattedString(conversation.language)
                        )
                    )
                    lastReset?.burnConversation(MessageLifetime.FLASH)
                    lastReset = conversation
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
                val start = parent.manager.timerData.timer
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
                val record = parent.manager.timerData.record
                conversation.replyMessage(
                    TelegramStrings.getString(
                        conversation.language,
                        "timer.recordInf",
                        record.toFormattedString(conversation.language)
                    )
                )
                conversation.burnConversation(MessageLifetime.SHORT)
            }

            override fun help(chat: Chat): String {
                return defaultHelp(TelegramStrings.getString(chat.getLocale(bot), "timer.record.help"))
            }
        }
    }
}