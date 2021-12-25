package org.koppakurhiev.janabot.telegram.services.patVpat

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.CommonStrings
import org.koppakurhiev.janabot.common.Strings
import org.koppakurhiev.janabot.common.getLogger
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.ABotCommandWithSubCommands
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.IBotSubCommand
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime
import org.koppakurhiev.janabot.telegram.services.patVpat.export.ExportAllQuestions
import org.koppakurhiev.janabot.telegram.services.patVpat.export.ExportAnswers
import org.koppakurhiev.janabot.telegram.services.patVpat.export.ExportEverything
import org.koppakurhiev.janabot.telegram.services.patVpat.export.ExportNew

class PatVPatCommand(override val service: PatVPatService) : ABotCommandWithSubCommands() {
    override val command = "5v5"

    override val subCommands = setOf(
        Subscribe(this),
        Unsubscribe(this),
        Reminders(this),
        AddQuestion(this),
        AddAnswer(this),
        CatchUp(this),
        Skip(this),
        Stats(this),
        Rules(this),
        Launch(this),
        Stop(this),
        Broadcast(this),
        ExportAllQuestions(this),
        ExportNew(this),
        ExportEverything(this),
        ExportAnswers(this)
    )

    override fun getUiCommand(): BotCommand {
        return BotCommand(command, "Get 5v5 game interface")
    }

    override fun getArguments(): Array<String> {
        return emptyArray()
    }

    override suspend fun onNoArguments(message: Message, argument: String?) {
        //TODO implement keyboard
        val conversation = Conversation(bot, message)
        conversation.replyMessage(TelegramStrings.getString(conversation.language, "noSubCommand", command))
        conversation.burnConversation(MessageLifetime.FLASH)
    }

    override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
        return false //TODO - implement, support for sub-commands
    }

    override fun plainHelp(chat: Chat): String? {
        return null
    }

    abstract class APatVPatSubCommand(override val parent: PatVPatCommand) : IBotSubCommand {
        val manager get() = parent.service.patVPatManager

        override fun getArguments(): Array<String> {
            return emptyArray()
        }

        protected suspend fun standardReply(
            operationResult: PatVPatManager.OperationResult,
            conversation: Conversation,
            onSuccess: (Strings.Locale) -> String
        ) {
            val text = when (operationResult) {
                PatVPatManager.OperationResult.ALREADY_SUBSCRIBED -> PatVPatStrings.getString(
                    conversation.language, "resubscribe"
                )
                PatVPatManager.OperationResult.NOT_SUBSCRIBED -> PatVPatStrings.getString(
                    conversation.language, "notSubscribed"
                )
                PatVPatManager.OperationResult.NOT_VALID_CHAT -> PatVPatStrings.getString(
                    conversation.language, "invalidChat"
                )
                PatVPatManager.OperationResult.NO_QUESTION_ASKED -> PatVPatStrings.getString(
                    conversation.language, "noQuestion"
                )
                PatVPatManager.OperationResult.LOAD_FAILED -> CommonStrings.getString(
                    conversation.language,
                    "db.loadFailed"
                )
                PatVPatManager.OperationResult.SAVE_FAILED -> CommonStrings.getString(
                    conversation.language,
                    "db.saveFailed"
                )
                PatVPatManager.OperationResult.SUCCESS -> onSuccess.invoke(conversation.language)
                else -> {
                    getLogger().error { "Unspecified error occurred!!" }
                    CommonStrings.getString(conversation.language, "unknownError")
                }
            }
            if (text.isNotBlank()) {
                conversation.replyMessage(text)
                if (operationResult != PatVPatManager.OperationResult.SUCCESS) {
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
            }
        }
    }
}