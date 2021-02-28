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
        logger.trace { "Executing command: $args" }
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
            "-qStatus".toLowerCase() -> printQuestionsStats(conversation)
            "-skip" -> skip(message.chat, conversation)
            "-catchup" -> catchUp(message.chat, conversation)
            "-export" -> export(args, conversation)
            "-rules" -> printRules(message.chat, conversation).burnConversation(MessageLifetime.MEDIUM)
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

    private suspend fun printQuestionsStats(conversation: Conversation) {
        val allQuestionCount = patVPatManager.getQuestionPoolSize()
        val askedCount = patVPatManager.getAskedQuestionsCount()
        val skippedCount = patVPatManager.getSkippedQuestionsCount()
        conversation.replyMessage(
            JanaBot.messages.get(
                "5v5.questionsStats",
                allQuestionCount,
                askedCount,
                skippedCount,
                allQuestionCount - askedCount
            )
        )
    }

    private suspend fun printRules(chat: Chat, conversation: Conversation? = null): Conversation {
        if (conversation == null) {
            return Conversation.startConversation(chat.id, JanaBot.messages.get("5v5.rules"))
        } else {
            conversation.sendMessage(JanaBot.messages.get("5v5.rules"))
        }
        return conversation
    }

    private suspend fun launchTheGame(conversation: Conversation) {
        val onSuccess = JanaBot.messages.get("5v5.launch")
        standardReply(patVPatManager.launch(), onSuccess, conversation)
    }

    private suspend fun subscribe(chat: Chat, userName: String, conversation: Conversation) {
        val onSuccess = JanaBot.messages.get("5v5.subscribed")
        val result = patVPatManager.subscribe(chat, userName)
        standardReply(result, onSuccess, conversation)
        if (result == PatVPatManager.OperationResult.SUCCESS) {
            printRules(chat, conversation)
            if (patVPatManager.isQuestionAsked()) {
                askAgain(chat, conversation)
            } else {
                conversation.sendMessage(JanaBot.messages.get("5v5.noQuestion"))
            }
        }
    }

    private suspend fun unsubscribe(chat: Chat, conversation: Conversation) {
        val onSuccess = JanaBot.messages.get("5v5.unsubscribed")
        standardReply(patVPatManager.unsubscribe(chat), onSuccess, conversation)
    }

    private suspend fun reminders(args: List<String>, chat: Chat, conversation: Conversation) {
        when (args.getArg(1)) {
            "on" -> standardReply(
                patVPatManager.remindersOn(chat),
                JanaBot.messages.get("5v5.remindersOn"),
                conversation
            )
            "off" -> standardReply(
                patVPatManager.remindersOff(chat),
                JanaBot.messages.get("5v5.remindersOff"),
                conversation
            )
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
        val onSuccess = JanaBot.messages.get("5v5.newQuestion", text, patVPatManager.getQuestionPoolSize() + 1)
        standardReply(patVPatManager.addQuestion(text, userName), onSuccess, conversation)
        conversation.burnConversation(MessageLifetime.FLASH)
    }

    private suspend fun askAgain(chat: Chat, conversation: Conversation) {
        standardReply(patVPatManager.askUser(chat.id), null, conversation)
        conversation.burnConversation(MessageLifetime.FLASH)
    }

    private suspend fun addAnswer(text: String?, chat: Chat, conversation: Conversation) {
        if (text == null || text.isBlank()) {
            conversation.replyMessage(JanaBot.messages.get("5v5.textNotFound"))
            conversation.burnConversation(MessageLifetime.FLASH)
            return
        }
        val onSuccess = JanaBot.messages.get("5v5.answerRecorded", text)
        standardReply(patVPatManager.addAnswer(chat, text), onSuccess, conversation)
    }

    private suspend fun skip(chat: Chat, conversation: Conversation) {
        standardReply(patVPatManager.skipQuestion(chat.id), null, conversation)
    }

    private fun export(args: List<String>, conversation: Conversation) {
        TODO()
    }

    private fun catchUp(chat: Chat, conversation: Conversation) {
        TODO()
    }

    override fun help(message: Message?): String {
        return if (message != null && patVPatManager.isSubscribed(message.chat.id)) {
            JanaBot.messages.get("5v5.help.subscribed")
        } else {
            JanaBot.messages.get("5v5.help.notSubscribed")
        }
    }

    private suspend fun standardReply(
        operationResult: PatVPatManager.OperationResult,
        onSuccess: String?,
        conversation: Conversation
    ) {
        val text = when (operationResult) {
            PatVPatManager.OperationResult.ALREADY_SUBSCRIBED -> JanaBot.messages.get("5v5.alreadySubscribed")
            PatVPatManager.OperationResult.NOT_SUBSCRIBED -> JanaBot.messages.get("5v5.notSubscribed")
            PatVPatManager.OperationResult.NOT_VALID_CHAT -> JanaBot.messages.get("5v5.invalidChat")
            PatVPatManager.OperationResult.NO_QUESTION_ASKED -> JanaBot.messages.get("5v5.noQuestion")
            PatVPatManager.OperationResult.LOAD_FAILED -> JanaBot.messages.get("db.loadFailed")
            PatVPatManager.OperationResult.SAVE_FAILED -> JanaBot.messages.get("db.saveFailed")
            PatVPatManager.OperationResult.SUCCESS -> onSuccess
            else -> {
                logger.error { "Unspecified error occurred!!" }
                JanaBot.messages.get("unknownError")
            }
        }
        if (text.isNullOrEmpty()) return
        conversation.replyMessage(text)
        if (operationResult != PatVPatManager.OperationResult.SUCCESS) conversation.burnConversation(MessageLifetime.FLASH)
    }
}