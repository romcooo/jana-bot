package org.koppakurhiev.janabot.telegram.services.subgroups

import com.elbekD.bot.types.BotCommand
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.Chat
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.common.CommonStrings
import org.koppakurhiev.janabot.common.Strings
import org.koppakurhiev.janabot.common.getArg
import org.koppakurhiev.janabot.common.getLocale
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.*

class SubGroupCommand(override val service: SubGroupsService) : ABotCommandWithSubCommands() {
    override val command = "group"

    override val subCommands: Set<IBotSubCommand> = setOf(
        Create(this),
        Delete(this),
        Rename(this),
        Join(this),
        Leave(this),
        Members(this),
        List(this),
        AddAdmin(this),
        RemoveAdmin(this),
        Admins(this),
        AddMember(this),
        Kick(this),
        ListAll(this)
    )

    override fun getUiCommand(): BotCommand? {
        return null //TODO
    }

    override suspend fun onNoArguments(message: Message, argument: String?) {
        //TODO Create user interface
        val conversation = Conversation(bot, message)
        conversation.replyMessage(TelegramStrings.getString(conversation.language, "noSubCommand", command))
        conversation.burnConversation(MessageLifetime.FLASH)
    }

    override fun plainHelp(chat: Chat): String? {
        return null //TODO update
    }

    override suspend fun onCallbackQuery(query: CallbackQuery, arguments: String): Boolean {
        return false//TODO
    }

    override fun getArguments(): Array<String> {
        return emptyArray()
    }

    private abstract class ASubGroupSubCommand(override val parent: SubGroupCommand) : IBotSubCommand {

        val manager: SubGroupsManager get() = parent.service.subGroupsManager

        protected suspend fun standardReply(
            operationResult: SubGroupsManager.OperationResult,
            conversation: Conversation,
            onSuccess: (locale: Strings.Locale) -> String
        ) {
            val locale = conversation.language
            val text = when (operationResult) {
                SubGroupsManager.OperationResult.GROUP_NOT_FOUND -> SubGroupStrings.getString(
                    locale,
                    "unknownGroup"
                )
                SubGroupsManager.OperationResult.GROUP_ALREADY_EXISTS -> SubGroupStrings.getString(
                    locale,
                    "nameTaken"
                )
                SubGroupsManager.OperationResult.GROUP_MISSING_MEMBER -> SubGroupStrings.getString(
                    locale,
                    "notMember"
                )
                SubGroupsManager.OperationResult.GROUP_CONTAINS_MEMBER -> SubGroupStrings.getString(
                    locale,
                    "alreadyMember"
                )
                SubGroupsManager.OperationResult.NOT_GROUP_ADMIN -> SubGroupStrings.getString(
                    locale,
                    "noPrivileges"
                )
                SubGroupsManager.OperationResult.LOAD_FAILED -> CommonStrings.getString(locale, "db.loadFailed")
                SubGroupsManager.OperationResult.SAVE_FAILED -> CommonStrings.getString(locale, "db.saveFailed")
                SubGroupsManager.OperationResult.UNKNOWN_ERROR -> CommonStrings.getString(locale, "unknownError")
                SubGroupsManager.OperationResult.SUCCESS -> onSuccess.invoke(locale)
            }
            conversation.replyMessage(text)
            if (operationResult != SubGroupsManager.OperationResult.SUCCESS) {
                conversation.burnConversation(MessageLifetime.SHORT)
            }
        }
    }

    private class Create(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "create"

        override fun getArguments(): Array<String> {
            return arrayOf("group name")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            val userId = message.from?.id?.toLong()
            when {
                arguments.isNullOrBlank() -> {
                    conversation.replyMessage(
                        TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                    )
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                arguments.contains(" ") -> {
                    conversation.replyMessage(SubGroupStrings.getString(conversation.language, "create.space"))
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                userId == null -> {
                    conversation.replyMessage(TelegramStrings.getString(conversation.language, "noUser"))
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                else -> {
                    val operationResult = manager.createSubGroup(arguments, conversation.chatId, userId)
                    standardReply(operationResult, conversation) {
                        SubGroupStrings.getString(it, "create", arguments)
                    }
                }
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "create.help"))
        }
    }

    private class Delete(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "delete"

        override fun getArguments(): Array<String> {
            return arrayOf("group name")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            val userId = message.from?.id?.toLong()
            when {
                arguments.isNullOrEmpty() -> {
                    conversation.replyMessage(
                        TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                    )
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                userId == null -> {
                    conversation.replyMessage(TelegramStrings.getString(conversation.language, "noUser"))
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                else -> {
                    var operationResult = manager.isGroupAdmin(conversation.chatId, arguments, userId)
                    if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                        operationResult = manager.deleteSubGroup(arguments, conversation.chatId)
                    }
                    standardReply(operationResult, conversation) {
                        SubGroupStrings.getString(conversation.language, "delete", arguments)
                    }
                }
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "delete.help"))
        }
    }

    private class Rename(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "rename"

        override fun getArguments(): Array<String> {
            return arrayOf("old name", "new name")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            val args = arguments?.split(" ")
            val oldGroupName = args?.getArg(0)
            val newGroupName = args?.getArg(1)
            val userId = message.from?.id?.toLong()
            when {
                userId == null -> {
                    conversation.replyMessage(TelegramStrings.getString(conversation.language, "noUser"))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
                (oldGroupName.isNullOrEmpty() || newGroupName.isNullOrEmpty()) -> {
                    conversation.replyMessage(
                        TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                    )
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
                else -> {
                    var operationResult = manager.isGroupAdmin(conversation.chatId, oldGroupName, userId)
                    if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                        operationResult = manager.renameSubGroup(oldGroupName, newGroupName, conversation.chatId)
                    }
                    standardReply(operationResult, conversation) {
                        SubGroupStrings.getString(conversation.language, "rename", oldGroupName, newGroupName)
                    }
                }
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "rename.help"))
        }
    }

    private class Join(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "join"

        override fun getArguments(): Array<String> {
            return arrayOf("group name")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val user = message.from
            val conversation = Conversation(bot, message)
            when {
                arguments.isNullOrBlank() -> {
                    conversation.replyMessage(
                        TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                    )
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                user == null -> {
                    conversation.replyMessage(TelegramStrings.getString(conversation.language, "noUser"))
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                else -> {
                    val operationResult = manager.addMember(arguments, conversation.chatId, user)
                    standardReply(operationResult, conversation) {
                        SubGroupStrings.getString(conversation.language, "members.add", user.username, arguments)
                    }
                    if (user.username.isNullOrBlank()) {
                        val warning = conversation.sendMessage(
                            SubGroupStrings.getString(conversation.language, "noUsername")
                        )
                        conversation.burnMessage(warning, MessageLifetime.SHORT)
                    }
                }
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "join.help"))
        }
    }

    private class Leave(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "leave"

        override fun getArguments(): Array<String> {
            return arrayOf("group name")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            val user = message.from
            when {
                arguments.isNullOrEmpty() -> {
                    conversation.replyMessage(
                        TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                    )
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                user == null -> {
                    conversation.replyMessage(TelegramStrings.getString(conversation.language, "noUser"))
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                else -> {
                    val operationResult = manager.removeMember(arguments, conversation.chatId, user)
                    standardReply(operationResult, conversation) {
                        SubGroupStrings.getString(it, "members.remove", user.username, arguments)
                    }
                }
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "leave.help"))
        }
    }

    private class Members(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "members"

        override fun getArguments(): Array<String> {
            return arrayOf("group name")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            if (arguments.isNullOrEmpty()) {
                conversation.replyMessage(
                    TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                )
                conversation.burnConversation(MessageLifetime.FLASH)
            } else {
                val members = manager.getMembersList(arguments, conversation.chatId)
                val text = when {
                    members == null -> SubGroupStrings.getString(conversation.language, "unknownGroup", arguments)
                    members.isEmpty() -> SubGroupStrings.getString(
                        conversation.language,
                        "members.none",
                        arguments
                    )
                    else -> SubGroupStrings.getString(
                        conversation.language, "print.subGroup", arguments, members.joinToString()
                    )
                }
                conversation.replyMessage(text)
                conversation.burnConversation(MessageLifetime.MEDIUM)
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "members.list.help"))
        }
    }

    private class List(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "list"

        override fun getArguments(): Array<String> {
            return emptyArray()
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            val subGroups = manager.getChatSubGroups(conversation.chatId)
            if (subGroups.isEmpty()) {
                conversation.replyMessage(SubGroupStrings.getString(conversation.language, "noGroups"))
                conversation.burnConversation(MessageLifetime.FLASH)
            } else {
                val subGroupsString = StringBuilder()
                subGroups.forEach {
                    val members = manager.getMembersList(it.name, message.chat.id)
                    if (members.isNullOrEmpty()) {
                        subGroupsString.appendLine(
                            SubGroupStrings.getString(
                                conversation.language,
                                "members.none",
                                it.name
                            )
                        )
                    } else {
                        subGroupsString.appendLine(
                            SubGroupStrings.getString(
                                conversation.language,
                                "print.subGroup",
                                it.name,
                                members.joinToString()
                            )
                        )
                    }
                }
                val text =
                    SubGroupStrings.getString(conversation.language, "print.chatGroups", subGroupsString.toString())
                conversation.replyMessage(text)
                conversation.burnConversation(MessageLifetime.MEDIUM)
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "list.help"))
        }
    }

    private class AddAdmin(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "addAdmin"

        override fun getArguments(): Array<String> {
            return arrayOf("group name", "new admin")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            val args = arguments?.split(" ")
            val subGroup = args?.getArg(0)
            val adminUsername = args?.getArg(1)
            val user = message.from
            when {
                user == null -> {
                    conversation.replyMessage(TelegramStrings.getString(conversation.language, "noUser"))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
                (subGroup.isNullOrBlank() || adminUsername.isNullOrBlank()) -> {
                    conversation.replyMessage(
                        TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                    )
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                else -> {
                    val newAdmin = bot.telegramBot.getAdmin(message.chat.id, adminUsername)
                    if (newAdmin == null) {
                        conversation.replyMessage(SubGroupStrings.getString(conversation.language, "noUsername"))
                        conversation.burnConversation(MessageLifetime.FLASH)
                    } else {
                        var result = manager.isGroupAdmin(message.chat.id, subGroup, user.id.toLong())
                        if (result == SubGroupsManager.OperationResult.SUCCESS) {
                            result = manager.addAdmin(message.chat.id, subGroup, newAdmin.id.toLong())
                        }
                        standardReply(result, conversation) {
                            SubGroupStrings.getString(conversation.language, "admins.add", adminUsername, subGroup)
                        }
                    }
                }
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "admins.add.help"))
        }
    }

    private class RemoveAdmin(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "removeAdmin"

        override fun getArguments(): Array<String> {
            return arrayOf("group name", "username")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            val args = arguments?.split(" ")
            val subGroup = args?.getArg(0)
            val adminUsername = args?.getArg(1)
            val user = message.from
            when {
                user == null -> {
                    conversation.replyMessage(TelegramStrings.getString(conversation.language, "noUser"))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
                (subGroup.isNullOrBlank() || adminUsername.isNullOrBlank()) -> {
                    conversation.replyMessage(
                        TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                    )
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                else -> {
                    val userToRemove = bot.telegramBot.getAdmin(message.chat.id, adminUsername)
                    if (userToRemove == null) {
                        conversation.replyMessage(SubGroupStrings.getString(conversation.language, "noUsername"))
                        conversation.burnConversation(MessageLifetime.FLASH)
                    } else {
                        var result = manager.isGroupAdmin(message.chat.id, subGroup, user.id.toLong())
                        if (result == SubGroupsManager.OperationResult.SUCCESS) {
                            result = manager.removeAdmin(message.chat.id, subGroup, userToRemove.id.toLong())
                        }
                        standardReply(result, conversation) {
                            SubGroupStrings.getString(conversation.language, "admins.remove", adminUsername, subGroup)
                        }
                    }
                }
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "admins.remove.help"))
        }
    }

    private class Admins(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "admins"

        override fun getArguments(): Array<String> {
            return arrayOf("group name")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val conversation = Conversation(bot, message)
            when {
                arguments.isNullOrEmpty() -> {
                    conversation.replyMessage(
                        TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                    )
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                else -> {
                    val admins = manager.getAdmins(conversation.chatId, arguments)
                    val text = when {
                        admins == null -> SubGroupStrings.getString(conversation.language, "unknownGroup")
                        admins.isEmpty() -> SubGroupStrings.getString(conversation.language, "admins.none")
                        else -> {
                            SubGroupStrings.getString(
                                conversation.language,
                                "admins.list",
                                admins.joinToString(", ")
                            )
                        }
                    }
                    conversation.replyMessage(text)
                    conversation.burnConversation(MessageLifetime.MEDIUM)
                }
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "admins.list.help"))
        }
    }

    private class AddMember(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "add"

        override fun getArguments(): Array<String> {
            return arrayOf("group name", "user")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val user = message.from
            val args = arguments?.split(" ")
            val groupName = args?.getArg(0)
            val userToAdd = args?.getArg(1)?.let { bot.telegramBot.getAdmin(message.chat.id, it) }
            val conversation = Conversation(bot, message)
            when {
                user == null -> {
                    conversation.replyMessage(TelegramStrings.getString(conversation.language, "noUser"))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
                groupName.isNullOrEmpty() -> {
                    conversation.replyMessage(
                        TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                    )
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                user.username.isNullOrEmpty() -> {
                    conversation.replyMessage(
                        SubGroupStrings.getString(
                            conversation.language,
                            "noUsername"
                        )
                    )
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
                userToAdd == null -> {
                    conversation.replyMessage(SubGroupStrings.getString(conversation.language, "wrongUsername"))
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                else -> {
                    var operationResult = manager.isGroupAdmin(conversation.chatId, groupName, user.id.toLong())
                    if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                        operationResult = manager.addMember(groupName, conversation.chatId, userToAdd)
                    }
                    standardReply(
                        operationResult,
                        conversation
                    ) {
                        SubGroupStrings.getString(
                            conversation.language,
                            "members.add",
                            userToAdd.username,
                            groupName
                        )
                    }
                }
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "members.add.help"))
        }
    }

    private class Kick(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "kick"

        override fun getArguments(): Array<String> {
            return arrayOf("group name", "user")
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            val user = message.from
            val args = arguments?.split(" ")
            val groupName = args?.getArg(0)
            val userToRemove = args?.getArg(1)?.let { bot.telegramBot.getAdmin(message.chat.id, it) }
            val conversation = Conversation(bot, message)
            when {
                user == null -> {
                    conversation.replyMessage(TelegramStrings.getString(conversation.language, "noUser"))
                    conversation.burnConversation(MessageLifetime.SHORT)
                }
                groupName.isNullOrEmpty() -> {
                    conversation.replyMessage(
                        TelegramStrings.onMissingArgument(conversation.language, getArguments(), command)
                    )
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                userToRemove == null -> {
                    conversation.replyMessage(
                        SubGroupStrings.getString(
                            conversation.language,
                            "wrongUsername"
                        )
                    )
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
                else -> {
                    var operationResult = manager.isGroupAdmin(conversation.chatId, groupName, user.id.toLong())
                    if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                        operationResult = manager.removeMember(groupName, conversation.chatId, userToRemove)
                    }
                    standardReply(operationResult, conversation) {
                        SubGroupStrings.getString(
                            conversation.language,
                            "members.remove",
                            userToRemove.username,
                            groupName
                        )
                    }
                }
            }
        }

        override fun help(chat: Chat): String {
            return defaultHelp(SubGroupStrings.getString(chat.getLocale(bot), "members.kick.help"))
        }
    }

    private class ListAll(parent: SubGroupCommand) : ASubGroupSubCommand(parent) {
        override val command = "listAll"

        override fun getArguments(): Array<String> {
            return emptyArray()
        }

        override suspend fun onCommand(message: Message, arguments: String?) {
            if (bot.isBotAdmin(message.from?.username)) {
                val groups = manager.getAllGroups()
                val conversation = Conversation(bot, message)
                if (groups.isNotEmpty()) {
                    conversation.replyMessage(
                        SubGroupStrings.getString(
                            conversation.language,
                            "print.allGroups",
                            groups.joinToString("\n")
                        )
                    )
                    conversation.burnConversation(MessageLifetime.MEDIUM)
                } else {
                    conversation.replyMessage(SubGroupStrings.getString(conversation.language, "noGroupsAll"))
                    conversation.burnConversation(MessageLifetime.FLASH)
                }
            }
        }

        override fun help(chat: Chat): String {
            return ""
        }
    }
}
