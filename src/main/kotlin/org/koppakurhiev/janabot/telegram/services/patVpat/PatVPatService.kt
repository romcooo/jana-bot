package org.koppakurhiev.janabot.telegram.services.patVpat

import com.elbekD.bot.types.Message
import org.jetbrains.annotations.PropertyKey
import org.koppakurhiev.janabot.common.*
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotCommand
import org.koppakurhiev.janabot.telegram.bot.IBotService
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot

class PatVPatService(override val bot: ITelegramBot) : IBotService {

    val patVPatManager = PatVPatManager(bot)

    override val commands: Array<IBotCommand> = arrayOf(
        PatVPatCommand(this)
    )

    override suspend fun onMessage(message: Message) {
        val replyTo = message.reply_to_message
        if (replyTo == null || !isPatVPatMessage(message.chat.getLocale(bot), replyTo)) return
        val questionText = patVPatManager.getCurrentQuestion()
        //Check it's a reply to recent 5v5 message
        if (questionText != null && !replyTo.text!!.contains(questionText)) {
            val conversation = Conversation(bot, message)
            conversation.replyMessage(PatVPatStrings.getString(conversation.language, "invalidReply"))
            return
        } else {
            //Process message
            val messageText = message.text
            if (patVPatManager.isSubscribed(message.chat.id) && messageText?.isNotEmpty() == true) {
                val conversation = Conversation(bot, message)
                val onSuccess = PatVPatStrings.getString(conversation.language, "addA", messageText)
                when (patVPatManager.addAnswer(message.chat, messageText)) {
                    PatVPatManager.OperationResult.SUCCESS -> conversation.replyMessage(onSuccess)
                    PatVPatManager.OperationResult.NO_QUESTION_ASKED ->
                        conversation.replyMessage(PatVPatStrings.getString(conversation.language, "noQuestion"))
                    PatVPatManager.OperationResult.SAVE_FAILED -> {
                        conversation.replyMessage(CommonStrings.getString(conversation.language, "db.saveFailed"))
                    }
                    else -> throw IllegalStateException("Unreachable code reached")
                }
            }
        }
    }

    private fun isPatVPatMessage(locale: Strings.Locale, message: Message): Boolean {
        val identifiers = PatVPatStrings.getString(locale, "messageIdentifier").split("|")
        return identifiers.any { message.text?.contains(it) ?: false }
    }
}

object PatVPatStrings : AStringsProvider("/PatVPat") {
    fun getString(
        locale: Strings.Locale = Strings.Locale.DEFAULT,
        @PropertyKey(resourceBundle = "PatVPat") key: String,
        vararg args: Any?
    ): String {
        return getStringsForLocale(locale).get(key, *args)
    }
}