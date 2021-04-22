package org.koppakurhiev.janabot.telegram.services.backpack

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.Message
import org.jetbrains.annotations.PropertyKey
import org.koppakurhiev.janabot.common.AStringsProvider
import org.koppakurhiev.janabot.common.Strings
import org.koppakurhiev.janabot.telegram.bot.*

class BackpackService(override val bot: ITelegramBot) : IBotService {
    override val commands = arrayOf<IBotCommand>(
        BackpackCommand(this)
    )

    val manager = BackpackManager(bot)

    override suspend fun onMessage(message: Message) {
        //Nothing
    }

    class BackpackCommand(override val service: BackpackService) : ABotCommandWithSubCommands() {
        override val command = "backpack"

        override val subCommands = setOf<IBotSubCommand>(
            CreateSubCommand(this),
            AddSubCommand(this),
            GetSubCommand(this),
            RandomSubCommand(this),
            DeleteSubCommand(this),
            RemoveSubCommand(this)
        )

        override fun getUiCommand(): BotCommand? {
            return null //TODO
        }

        override fun getArguments(): Array<String> {
            return emptyArray()
        }

        override suspend fun onNoArguments(message: Message, argument: String?) {
            val conversation = Conversation(bot, message)
            val backpacks = service.manager.getAll(message.chat.id)
            conversation.replyMessage(
                BackpackStrings.getString(
                    conversation.language,
                    "getAll",
                    backpacks.joinToString("\n")
                )
            )
        }

        override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
            return false
        }
    }
}

object BackpackStrings : AStringsProvider("/Backpack") {
    fun getString(
        locale: Strings.Locale = Strings.Locale.DEFAULT,
        @PropertyKey(resourceBundle = "Backpack") key: String,
        vararg args: Any?
    ): String {
        return getStringsForLocale(locale).get(key, *args)
    }
}