package org.koppakurhiev.janabot.services.patVpat

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.utils.getArg

class PatVPatCommand(private val patVPatManager: PatVPatManager) : ABotService.ACommand("/5v5") {
    override suspend fun onCommand(message: Message, s: String?) {
        val conversation = Conversation(message)
        val args = message.text?.split(" ")?.drop(1)
        if (args == null || args.isEmpty()) {
            conversation.replyMessage(JanaBot.messages.get("5v5.nothing"))
            conversation.burnConversation(MessageLifetime.FLASH)
            return
        }
        logger.debug { "Executing command: $args" }
        when (args[0].toLowerCase()) {
            "-launch" -> launchTheGame(conversation)
            "-subscribe" -> {
                val usersName = "${message.from?.first_name} ${message.from?.last_name}"
                subscribe(message.chat, usersName, conversation)
            }
            "-unsubscribe" -> unsubscribe(message.chat, conversation)
            "-reminders" -> reminders(args, message.chat, conversation)
            "-addQ".toLowerCase() -> {
                val questionText = message.text?.replace("/5v5 -addQ", "", true)?.trim()
                val usersName = "${message.from?.first_name} ${message.from?.last_name}"
                addQuestion(questionText, usersName, conversation)
            }
            "-addA".toLowerCase() -> {
                val answerText = message.text?.replace("/5v5 -addA", "", true)?.trim()
                addAnswer(answerText, message.chat, conversation)
            }
            "-again" -> askAgain(message.chat, conversation)
            "-skip" -> skip(conversation)
            "-catchup" -> TODO()
            "-export" -> TODO()
            "-help" -> {
                conversation.replyMessage(help(message))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
            else -> {
                conversation.replyMessage(JanaBot.messages.get("unknownCommand", args.getArg(1)))
                conversation.burnConversation(MessageLifetime.FLASH)
                return
            }
        }
    }

    private suspend fun launchTheGame(conversation: Conversation) {
        when (patVPatManager.launch()) {
            PatVPatManager.OperationResult.SUCCESS -> conversation.replyMessage(JanaBot.messages.get("5v5.launch"))
            else -> {
                conversation.replyMessage(JanaBot.messages.get("unknownError"))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
        }
    }

    private suspend fun subscribe(chat: Chat, userName: String, conversation: Conversation) {
        when (patVPatManager.subscribe(chat, userName)) {
            PatVPatManager.OperationResult.SUCCESS -> {
                conversation.replyMessage(JanaBot.messages.get("5v5.subscribed"))
                if (patVPatManager.isQuestionAsked()) {
                    askAgain(chat, conversation)
                } else {
                    conversation.replyMessage(JanaBot.messages.get("5v5.noQuestion"))
                }
            }
            PatVPatManager.OperationResult.SAVE_FAILED -> {
                conversation.replyMessage(JanaBot.messages.get("db.saveFailed"))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
            else -> {
                conversation.replyMessage(JanaBot.messages.get("unknownError"))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
        }
    }

    private suspend fun unsubscribe(chat: Chat, conversation: Conversation) {
        when (patVPatManager.unsubscribe(chat)) {
            PatVPatManager.OperationResult.SUCCESS -> {
                conversation.replyMessage(JanaBot.messages.get("5v5.unsubscribed"))
            }
            PatVPatManager.OperationResult.SAVE_FAILED -> {
                conversation.replyMessage(JanaBot.messages.get("db.saveFailed"))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
            else -> {
                conversation.replyMessage(JanaBot.messages.get("unknownError"))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
        }
    }

    private suspend fun reminders(args: List<String>, chat: Chat, conversation: Conversation) {
        when (args.getArg(1)) {
            "on" -> when (patVPatManager.remindersOn(chat)) {
                PatVPatManager.OperationResult.SUCCESS -> conversation.replyMessage(JanaBot.messages.get("5v5.remindersOn"))
                else -> {
                    conversation.replyMessage(JanaBot.messages.get("unknownError"))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
            }
            "off" -> when (patVPatManager.remindersOff(chat)) {
                PatVPatManager.OperationResult.SUCCESS -> conversation.replyMessage(JanaBot.messages.get("5v5.remindersOff"))
                else -> {
                    conversation.replyMessage(JanaBot.messages.get("unknownError"))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
            }
            else -> {
                conversation.replyMessage(JanaBot.messages.get("5v5.onOrOff"))
                conversation.burnConversation(MessageLifetime.FLASH)
            }
        }
    }

    private suspend fun addQuestion(text: String?, userName: String, conversation: Conversation) {
        if (text == null || text.isBlank()) {
            conversation.replyMessage(JanaBot.messages.get("5v5.textNotFound"))
            conversation.burnConversation(MessageLifetime.FLASH)
            return
        }
        when (patVPatManager.addQuestion(text, userName)) {
            PatVPatManager.OperationResult.SUCCESS -> {
                conversation.replyMessage(JanaBot.messages.get("5v5.newQuestion", text, 0))
                conversation.burnConversation(MessageLifetime.MEDIUM)
            }
            else -> {
                conversation.replyMessage(JanaBot.messages.get("unknownError"))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
        }
    }

    private suspend fun askAgain(chat: Chat, conversation: Conversation) {
        when (patVPatManager.askUser(chat)) {
            PatVPatManager.OperationResult.SUCCESS -> conversation.burnConversation(MessageLifetime.FLASH)
            PatVPatManager.OperationResult.NOT_SUBSCRIBED -> {
                conversation.replyMessage(JanaBot.messages.get("5v5.notSubscribed"))
            }
            PatVPatManager.OperationResult.FAILURE -> {
                conversation.replyMessage(JanaBot.messages.get("5v5.noQuestion"))
            }
            else -> {
                conversation.replyMessage(JanaBot.messages.get("unknownError"))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
        }
    }

    private suspend fun addAnswer(text: String?, chat: Chat, conversation: Conversation) {
        if (text == null || text.isBlank()) {
            conversation.replyMessage(JanaBot.messages.get("5v5.textNotFound"))
            conversation.burnConversation(MessageLifetime.FLASH)
            return
        }
        when (patVPatManager.addAnswer(text, chat)) {
            PatVPatManager.OperationResult.SUCCESS -> {
                conversation.replyMessage(JanaBot.messages.get("5v5.answerRecorded"))
            }
            else -> {
                conversation.replyMessage(JanaBot.messages.get("unknownError"))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
        }
    }

    private suspend fun skip(conversation: Conversation) {
        when (patVPatManager.skipQuestion()) {
            PatVPatManager.OperationResult.SUCCESS -> conversation.replyMessage(JanaBot.messages.get("5v5.skip"))
            else -> {
                conversation.replyMessage(JanaBot.messages.get("unknownError"))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
        }
    }

    private fun export() {
        TODO()
    }

    private fun catchUp() {
        TODO()
    }

    override fun help(message: Message?): String {
        return if (message != null && patVPatManager.isSubscribed(message.chat)) {
            JanaBot.messages.get("5v5.help.subscribed")
        } else {
            JanaBot.messages.get("5v5.help.notSubscribed")
        }
    }
}