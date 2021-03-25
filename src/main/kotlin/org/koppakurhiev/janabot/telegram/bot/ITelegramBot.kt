package org.koppakurhiev.janabot.telegram.bot

import com.elbekD.bot.Bot
import com.elbekD.bot.http.TelegramApiError
import com.elbekD.bot.http.await
import mu.KotlinLogging
import org.koppakurhiev.janabot.IBot

interface ITelegramBot : IBot {

    val services: Set<IBotService>

    fun getBot(): Bot

    fun isBotAdmin(username: String): Boolean
}

suspend fun Bot.getAdminId(chatId: Any, username: String): Int? {
    try {
        val chatAdmins = getChatAdministrators(chatId).await()
        val user = chatAdmins.find { it.user.username.equals(username) }?.user
        return user?.id
    } catch (exception: TelegramApiError) {
        KotlinLogging.logger {}.warn { "Failed to obtain admins - ${exception.message}" }
    }
    return null
}