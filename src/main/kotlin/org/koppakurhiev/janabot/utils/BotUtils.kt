package org.koppakurhiev.janabot.utils

import com.elbekD.bot.Bot
import com.elbekD.bot.http.TelegramApiError
import com.elbekD.bot.http.await
import mu.KotlinLogging

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
