package org.koppakurhiev.janabot.telegram.services.patVpat.export

import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.bson.types.ObjectId
import org.koppakurhiev.janabot.common.getArg
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.services.patVpat.PatVPatCommand
import org.koppakurhiev.janabot.telegram.services.patVpat.PatVPatStrings
import org.koppakurhiev.janabot.telegram.services.patVpat.data.Question
import org.litote.kmongo.id.toId

private fun PatVPatCommand.APatVPatSubCommand.getExportManager() =
    parent.service.patVPatExportManager

private const val LINK = "https://docs.google.com/spreadsheets/d/%s"

class ExportAllQuestions(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "exportQuestions"

    private val sheetId = "1ehjaoDorij6Az7Iy9w_s1Y_l8Sy3NXCc2r9J_HGPCXY"

    override suspend fun onCommand(message: Message, arguments: String?) {
        val user = message.from?.username
        if (user != null && bot.isBotAdmin(user)) {
            val conversation = Conversation(bot, message)
            conversation.replyMessage(
                if (getExportManager().exportQuestions(sheetId)) {
                    PatVPatStrings.getString(conversation.language, "export.successful", LINK.format(sheetId))
                } else {
                    PatVPatStrings.getString(conversation.language, "export.failed")
                }
            )
        }
    }

    override fun help(chat: Chat): String {
        return ""
    }
}

class ExportNew(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "exportNew"

    private val sheetId = "1ehjaoDorij6Az7Iy9w_s1Y_l8Sy3NXCc2r9J_HGPCXY"

    override suspend fun onCommand(message: Message, arguments: String?) {
        val user = message.from?.username
        if (user != null && bot.isBotAdmin(user)) {
            val conversation = Conversation(bot, message)
            conversation.replyMessage(
                if (getExportManager().exportAllWithAnswers(sheetId, false)) {
                    PatVPatStrings.getString(conversation.language, "export.successful", LINK.format(sheetId))
                } else {
                    PatVPatStrings.getString(conversation.language, "export.failed")
                }
            )
        }
    }

    override fun help(chat: Chat): String {
        return ""
    }
}

class ExportEverything(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "exportEverything"

    private val sheetId = "1ehjaoDorij6Az7Iy9w_s1Y_l8Sy3NXCc2r9J_HGPCXY"

    override suspend fun onCommand(message: Message, arguments: String?) {
        val user = message.from?.username
        if (user != null && bot.isBotAdmin(user)) {
            val conversation = Conversation(bot, message)
            conversation.replyMessage(
                if (getExportManager().exportAllWithAnswers(sheetId, true)) {
                    PatVPatStrings.getString(conversation.language, "export.successful", LINK.format(sheetId))
                } else {
                    PatVPatStrings.getString(conversation.language, "export.failed")
                }
            )
        }
    }

    override fun help(chat: Chat): String {
        return ""
    }
}

class ExportAnswers(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "export"

    private val sheetId = "1ehjaoDorij6Az7Iy9w_s1Y_l8Sy3NXCc2r9J_HGPCXY"

    override fun getArguments(): Array<String> {
        return arrayOf("number of questions")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val params = arguments?.split(" ")
        val n = params?.getArg(0)?.toIntOrNull()
        val conversation = Conversation(bot, message)
        if (n == null || n <= 0) {
            conversation.replyMessage(
                TelegramStrings.onMissingArgument(
                    conversation.language,
                    getArguments(),
                    command
                )
            )
            return
        }
        val user = message.from?.username
        if (user != null && bot.isBotAdmin(user)) {
            conversation.replyMessage(
                if (getExportManager().exportAnswers(sheetId, n)) {
                    PatVPatStrings.getString(conversation.language, "export.successful", LINK.format(sheetId))
                } else {
                    PatVPatStrings.getString(conversation.language, "export.failed")
                }
            )
        }
    }

    override fun help(chat: Chat): String {
        return ""
    }
}

class ExportQuestion(parent: PatVPatCommand) : PatVPatCommand.APatVPatSubCommand(parent) {
    override val command = "exportOne"

    private val sheetId = "1c5rymGm1WDVNYGlEkrC0-zzN5xhuB75819OaY89iS54"

    override fun getArguments(): Array<String> {
        return arrayOf("number of questions")
    }

    override suspend fun onCommand(message: Message, arguments: String?) {
        val params = arguments?.split(" ")
        val id = params?.getArg(0)
        val conversation = Conversation(bot, message)
        if (id.isNullOrBlank()) {
            conversation.replyMessage(
                TelegramStrings.onMissingArgument(
                    conversation.language,
                    getArguments(),
                    command
                )
            )
            return
        }
        val questionId = ObjectId(id).toId<Question>()
        val user = message.from?.username
        if (user != null && bot.isBotAdmin(user)) {
            conversation.replyMessage(
                if (getExportManager().exportOneQuestion(sheetId, questionId)) {
                    PatVPatStrings.getString(conversation.language, "export.successful", LINK.format(sheetId))
                } else {
                    PatVPatStrings.getString(conversation.language, "export.failed")
                }
            )
        }
    }

    override fun help(chat: Chat): String {
        return ""
    }
}