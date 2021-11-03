package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.Bot
import com.elbekD.bot.http.TelegramApiError
import com.elbekD.bot.http.await
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.User
import mu.KotlinLogging
import org.koppakurhiev.janabot.IBot

interface ITelegramBot : IBot {

    val services: Set<IBotService>
    val telegramBot: Bot

    fun isBotAdmin(username: String?): Boolean

    suspend fun onCallbackQuery(query: CallbackQuery, arguments: String?)
}

suspend fun Bot.getAdmin(chatId: Any, username: String): User? {
    try {
        val chatAdmins = getChatAdministrators(chatId).await()
        return chatAdmins.find { it.user.username.equals(username) }?.user
    } catch (exception: TelegramApiError) {
        KotlinLogging.logger {}.error(exception) { "Failed to obtain admins" }
    }
    return null
}

suspend fun Bot.getUsername(chatId: Long, userId: Long): String? {
    try {
        return this.getChatMember(chatId, userId).await().user.username
    } catch (exception: TelegramApiError) {
        KotlinLogging.logger {}.error(exception) { "Failed to obtain users username" }
    }
    return null
}