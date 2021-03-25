package org.koppakurhiev.janabot.telegram.services.patVpat

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.CommonStrings
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotService
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.koppakurhiev.janabot.telegram.services.ABotService

class PatVPatService(bot: ITelegramBot) : ABotService(bot) {

    val patVPatManager = PatVPatManager(bot)

    override fun getCommands(): Array<IBotService.IBotCommand> {
        return arrayOf(
            PatVPatCommand(this)
        )
    }

    override suspend fun onMessage(message: Message) {
        //Needs refactor
        val conversation = Conversation(bot, message)
        val text = message.text
        val replyTo = message.reply_to_message
        if (replyTo == null || !isPatVPatMessage(replyTo)) return
        val questionText = patVPatManager.getCurrentQuestion()
        if (questionText != null && !replyTo.text!!.contains(questionText)) {
            conversation.replyMessage(TelegramStrings.getString("5v5.invalidReply"))
            return
        }
        if (patVPatManager.isSubscribed(message.chat.id) && text != null && text.isNotBlank()) {

            val onSuccess = TelegramStrings.getString("5v5.answerRecorded", text)
            when (patVPatManager.addAnswer(message.chat, text)) {
                PatVPatManager.OperationResult.SUCCESS -> conversation.replyMessage(onSuccess)
                PatVPatManager.OperationResult.NO_QUESTION_ASKED ->
                    conversation.replyMessage(TelegramStrings.getString("5v5.noQuestion"))
                PatVPatManager.OperationResult.SAVE_FAILED -> {
                    conversation.replyMessage(CommonStrings.getString("db.saveFailed"))
                }
                else -> {
                    //Ignore otherwise
                }
            }
        }
    }

    private fun isPatVPatMessage(message: Message): Boolean {
        val identifiers = TelegramStrings.getString("5v5.messageIdentifier").split("|")
        return identifiers.any { message.text?.contains(it) ?: false }
    }
}