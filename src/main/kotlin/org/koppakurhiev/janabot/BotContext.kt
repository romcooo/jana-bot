package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import org.koppakurhiev.janabot.core.BotBuilder
import org.koppakurhiev.janabot.core.GroupsManager

object BotContext {

    lateinit var bot: Bot
    val groupsManager: GroupsManager = GroupsManager.defaultImplementation()

    fun launch() {
        bot = BotBuilder().buildDefault()
        bot.start()
        println("Bot started.")
    }
}