package org.koppakurhiev.janabot.services.timer

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.services.IBotService
import org.koppakurhiev.janabot.utils.getArg
import org.koppakurhiev.janabot.utils.toFormattedString
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TimerService : ABotService() {

    private val timerManager = TimerManager()

    override fun getCommands(): Array<IBotService.ICommand> {
        return arrayOf(
            TimerCommand(timerManager)
        )
    }

    class TimerCommand(private val timerManager: TimerManager) : ABotService.ACommand("/timer") {

        private var lastReset : Conversation? = null

        override suspend fun onCommand(message: Message, s: String?) {
            val conversation = Conversation(message)
            val args = message.text?.split(" ")?.drop(1)
            logger.debug { "Executing command: $args" }
            if (args == null || args.isEmpty()) {
                getReport(conversation)
                return
            }
            when (args.getArg(0)) {
                "-status" -> getReport(conversation)
                "-reset" -> resetTimer(conversation)
                "-record" -> getRecord(conversation)
                "-help" -> {
                    conversation.replyMessage(help())
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
                else -> {
                    conversation.replyMessage(JanaBot.messages.get("timer.unknownCommand", args.getArg(0)))
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
            }
        }

        private suspend fun resetTimer(conversation: Conversation) {
            val oldRecord = timerManager.timerData.record
            when (timerManager.resetTimer()) {
                TimerManager.OperationResult.SUCCESS -> {
                    conversation.replyMessage(
                        JanaBot.messages.get(
                            "timer.reset",
                            timerManager.timerData.lastRunLength.toFormattedString()
                        )
                    )
                    lastReset?.burnConversation(MessageLifetime.FLASH)
                    lastReset = conversation
                }
                TimerManager.OperationResult.RECORD_BROKEN -> {
                    conversation.replyMessage(
                        JanaBot.messages.get(
                            "timer.reset",
                            timerManager.timerData.lastRunLength.toFormattedString()
                        )
                    )
                    conversation.sendMessage(JanaBot.messages.get("timer.recordBroken", oldRecord.toFormattedString()))
                    lastReset?.burnConversation(MessageLifetime.FLASH)
                    lastReset = conversation
                }
                TimerManager.OperationResult.SAVE_FAILED -> {
                    conversation.replyMessage(JanaBot.messages.get("db.saveFailed"))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
            }
        }

        private suspend fun getRecord(conversation: Conversation) {
            val record = timerManager.timerData.record
            conversation.replyMessage(JanaBot.messages.get("timer.recordInf", record.toFormattedString()))
            conversation.burnConversation(MessageLifetime.SHORT)
        }

        private suspend fun getReport(conversation: Conversation) {
            val start = timerManager.timerData.timer
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val since = formatter.format(start)
            val runningFor = Duration.between(start, LocalDateTime.now())
            conversation.replyMessage(JanaBot.messages.get("timer.status", since, runningFor.toFormattedString()))
            conversation.burnConversation(MessageLifetime.SHORT)
        }

        override fun help(): String {
            return JanaBot.messages.get("timer.help")
        }
    }
}