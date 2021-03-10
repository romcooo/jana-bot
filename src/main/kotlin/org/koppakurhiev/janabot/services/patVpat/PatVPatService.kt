package org.koppakurhiev.janabot.services.patVpat

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.services.IBotService

class PatVPatService : ABotService() {

    private val patVPatManager = PatVPatManager()

    override fun getCommands(): Array<IBotService.ICommand> {
        return arrayOf(
            PatVPatCommand(patVPatManager)
        )
    }

    override suspend fun onMessage(message: Message) {
        val text = message.text
        val replyTo = message.reply_to_message
        if (replyTo == null || !isPatVPatMessage(replyTo)) return
        if (patVPatManager.isSubscribed(message.chat.id) && text != null && text.isNotBlank()) {
            val conversation = Conversation(message)
            val onSuccess = JanaBot.messages.get("5v5.answerRecorded", text)
            when (patVPatManager.addAnswer(message.chat, text)) {
                PatVPatManager.OperationResult.SUCCESS -> conversation.replyMessage(onSuccess)
                PatVPatManager.OperationResult.NO_QUESTION_ASKED ->
                    conversation.replyMessage(JanaBot.messages.get("5v5.noQuestion"))
                PatVPatManager.OperationResult.SAVE_FAILED -> {
                    conversation.replyMessage(JanaBot.messages.get("db.saveFailed"))
                }
                else -> {
                    //Ignore otherwise
                }
            }
        }
    }

    private fun isPatVPatMessage(message: Message): Boolean {
        val identifiers = JanaBot.messages.get("5v5.messageIdentifier").split("|")
        return identifiers.any { message.text?.contains(it) ?: false }
    }
}