package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.CallbackQuery

interface IBotSubCommand : IBotCommand {
    val parent: IBotCommand
    override val service get() = parent.service

    override fun getUiCommand(): BotCommand? {
        return null
    }

    override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
        return false
    }
}