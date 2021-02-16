package org.koppakurhiev.janabot.utils

import com.elbekD.bot.Bot
import com.elbekD.bot.http.await

suspend fun Bot.getAdminId(chatId: Any, username: String): Int? {
    val chatAdmins = getChatAdministrators(chatId).await()
    val user = chatAdmins.find { it.user.username.equals(username) }?.user
    return user?.id
}
