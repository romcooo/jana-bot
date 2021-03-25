package org.koppakurhiev.janabot.telegram.services.timer

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.CommonStrings
import org.koppakurhiev.janabot.common.Duration
import org.koppakurhiev.janabot.common.getArg
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotService
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime
import org.koppakurhiev.janabot.telegram.services.ABotService
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TimerService(bot: ITelegramBot) : ABotService(bot) {

    val timerManager = TimerManager(bot)

    override fun getCommands(): Array<IBotService.IBotCommand> {
        return arrayOf(
            TimerCommand(this)
        )
    }

    class TimerCommand(service: TimerService) : ABotService.ABotCommand(service, "/timer") {

        private val timerManager = service.timerManager
        private var lastReset: Conversation? = null

        override fun getUiCommands(): List<BotCommand> {
            return listOf(BotCommand("timer -reset", "Resets THE timer"))
        }

        override suspend fun onCommand(message: Message, s: String?) {
            val conversation = Conversation(service.bot, message)
            val args = message.text?.split(" ")?.drop(1)
            logger.debug { "Executing command: $args" }
            if (args == null || args.isEmpty()) {
                resetTimer(conversation)
                return
            }
            when (args.getArg(0)) {
                "-info" -> getReport(conversation)
                "-reset" -> resetTimer(conversation)
                "-record" -> getRecord(conversation)
                "-help" -> {
                    conversation.replyMessage(help(message))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
                else -> {
                    conversation.replyMessage(CommonStrings.getString("unknownCommand", args.getArg(0)))
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
            }
        }

        private suspend fun resetTimer(conversation: Conversation) {
            val oldRecord = timerManager.timerData.record
            when (timerManager.resetTimer()) {
                TimerManager.OperationResult.SUCCESS -> {
                    conversation.replyMessage(
                        TelegramStrings.getString(
                            "timer.reset",
                            timerManager.timerData.lastRunLength.toFormattedString()
                        )
                    )
                    lastReset?.burnConversation(MessageLifetime.FLASH)
                    lastReset = conversation
                }
                TimerManager.OperationResult.RECORD_BROKEN -> {
                    conversation.replyMessage(
                        TelegramStrings.getString(
                            "timer.reset",
                            timerManager.timerData.lastRunLength.toFormattedString()
                        )
                    )
                    conversation.sendMessage(
                        TelegramStrings.getString(
                            "timer.recordBroken",
                            oldRecord.toFormattedString()
                        )
                    )
                    lastReset?.burnConversation(MessageLifetime.FLASH)
                    lastReset = conversation
                }
                TimerManager.OperationResult.SAVE_FAILED -> {
                    conversation.replyMessage(CommonStrings.getString("db.saveFailed"))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
            }
        }

        private suspend fun getRecord(conversation: Conversation) {
            val record = timerManager.timerData.record
            conversation.replyMessage(TelegramStrings.getString("timer.recordInf", record.toFormattedString()))
            conversation.burnConversation(MessageLifetime.SHORT)
        }

        private suspend fun getReport(conversation: Conversation) {
            val start = timerManager.timerData.timer
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val since = formatter.format(start)
            val runningFor = Duration.between(
                start.toEpochSecond(ZoneOffset.UTC),
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            )
            conversation.replyMessage(TelegramStrings.getString("timer.status", since, runningFor.toFormattedString()))
            conversation.burnConversation(MessageLifetime.SHORT)
        }

        override fun help(message: Message?): String {
            return TelegramStrings.getString("timer.help")
        }
    }
}