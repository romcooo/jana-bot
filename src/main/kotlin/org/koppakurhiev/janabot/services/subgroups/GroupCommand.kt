package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.SimpleConversationContext
import org.koppakurhiev.janabot.features.LivingMessage
import org.koppakurhiev.janabot.persistence.Repository.OperationResultListener
import org.koppakurhiev.janabot.sendMessage
import org.koppakurhiev.janabot.services.ABotService

class GroupCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/group") {
    private lateinit var conversationContext: SimpleConversationContext

    override fun onCommand(message: Message, s: String?) {
        // TODO check thread safety
        conversationContext = SimpleConversationContext(message.chat.id, message.message_id)
        val words = message.text?.split(" ")
        logger.debug { "Executing command: $words" }
        if (words == null || words.size < 2) {
            conversationContext.sendMessage(MessageProvider.noCommand(), lifetime = LivingMessage.MessageLifetime.SHORT)
            return
        }
        val targetGroupName: String? = if (words.size >= 3) words[2] else null
        val additionalArguments: List<String?>? = if (words.size >= 4) words.subList(3, words.size) else null
        try {
            when (words[1].toLowerCase()) {
                "-create" -> createSubGroup(targetGroupName, message)
                "-join" -> joinSubGroup(targetGroupName, message)
                "-leave" -> leaveSubGroup(targetGroupName, message)
                "-kick" -> removeMember(targetGroupName, words[3], message)
//                "-add" -> addMember(targetGroupName, words[3], message) // See the method for why it's disabled
                "-delete" -> deleteSubGroup(targetGroupName, message)
                "-members" -> getSubGroupMembers(targetGroupName, message)
                "-list" -> getChatSubgroups(message)
                // Use for testing purposes TODO - consider deleting later (but it is kind of useful)
                "-listAll".toLowerCase() -> getAllSubGroups(message)
                "-rename" -> renameSubGroup(targetGroupName, words[3], message) // TODO words[].. rework ??
                "-backup", "-backups" -> {
                    when (words[2].toLowerCase()) {
                        "-save" -> backupSubGroups()
                        "-list" -> getAvailableBackups(message)
                    }
                }
//                "-admin", "-admins" -> when (words[3].toLowerCase()) {
//                    "-add" -> TODO("Not yet implemented")
//                    "-leave" -> TODO("Not yet implemented")
//                }
                // TODO allow loading a backup directly
                //            "-load" -> loadSubGroups(filename)
                else -> {
                    logger.trace { "Unknown argument used: ${words[1]}" }
                    conversationContext.sendMessage(
                        text = MessageProvider.unrecognizedArgument(words[1]),
                        lifetime = LivingMessage.MessageLifetime.SHORT
                    )
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            logger.info { "Wrong number of arguments for given command $words" }
            conversationContext.sendMessage(
                text = MessageProvider.notEnoughArguments(words[1]),
                lifetime = LivingMessage.MessageLifetime.SHORT
            )
        }
        JanaBot.messageCleaner.registerMessage(
            LivingMessage(
                chatId = message.chat.id,
                messageId = message.message_id,
                lifetime = LivingMessage.MessageLifetime.SHORT
            )
        )

    }

    private fun createSubGroup(groupName: String?, message: Message) {
        val text = if (groupName.isNullOrEmpty()) {
            MessageProvider.noGroupName()
        } else {
            if (subGroupsManager.createSubGroup(groupName, message.chat.id, message.from?.id)) {
                MessageProvider.groupCreated(groupName, message.from?.first_name)
            } else {
                MessageProvider.groupExists(groupName)
            }
        }
        conversationContext.sendMessage(text, lifetime = LivingMessage.MessageLifetime.LONG)
    }

    private fun joinSubGroup(groupName: String?, message: Message) {
        val username = message.from?.username
        val text = when {
            groupName.isNullOrEmpty() -> MessageProvider.noGroupName()
            username.isNullOrEmpty() -> MessageProvider.noUsername()
            subGroupsManager.getSubGroup(message.chat.id, groupName) == null ->
                MessageProvider.groupNotFound(groupName)
            subGroupsManager.addMember(groupName, message.chat.id, username) ->
                MessageProvider.userAddedToGroup(username, groupName)
            else -> MessageProvider.userInGroup(username, groupName)
        }
        conversationContext.sendMessage(text, lifetime = LivingMessage.MessageLifetime.LONG)
    }

    // TODO currently not possible to validate, if the usernameToAdd is actually in the chat. Probably
    //          will need to rework the SubGroup to store user IDs as well in the future for this to be possible.
    private fun addMember(groupName: String?, usernameToAdd: String?, message: Message) {
        val requestorUsername = message.from?.username
        val text = when {
            groupName.isNullOrEmpty() -> MessageProvider.noGroupName()
            requestorUsername.isNullOrEmpty() -> MessageProvider.noUsername()
            usernameToAdd.isNullOrEmpty() -> MessageProvider.noUsernameProvided()
            subGroupsManager.getSubGroup(message.chat.id, groupName) == null -> MessageProvider.groupNotFound(groupName)
            subGroupsManager.addMember(groupName, message.chat.id, usernameToAdd) ->
                MessageProvider.userAddedToGroup(usernameToAdd, groupName)
            else -> MessageProvider.userInGroup(usernameToAdd, groupName)
        }
        conversationContext.sendMessage(text, lifetime = LivingMessage.MessageLifetime.LONG)
    }

    private fun removeMember(groupName: String?, usernameToRemove: String?, message: Message) {
        val requestorUsername = message.from?.username
        val text = when {
            groupName.isNullOrEmpty() -> MessageProvider.noGroupName()
            requestorUsername.isNullOrEmpty() -> MessageProvider.noUsername()
            usernameToRemove.isNullOrEmpty() -> MessageProvider.noUsernameProvided()
            else -> {
                val group = subGroupsManager.getSubGroup(message.chat.id, groupName)
                when {
                    group == null -> MessageProvider.groupNotFound(groupName)
                    !group.admins.contains(message.from?.id) -> {
                        val adminUsernames = group.admins.map {
                            JanaBot.bot.getChatMember(message.chat, it.toLong()).get().user.username
                        }
                        MessageProvider.noPrivileges(groupName, adminUsernames)
                    }
                    subGroupsManager.removeMember(groupName, message.chat.id, usernameToRemove) ->
                        MessageProvider.userRemovedFromGroupBy(requestorUsername, requestorUsername, groupName)
                    else -> MessageProvider.userNotInGroup(usernameToRemove, groupName)
                }
            }
        }
        conversationContext.sendMessage(text, lifetime = LivingMessage.MessageLifetime.LONG)
    }

    private fun leaveSubGroup(groupName: String?, message: Message) {
        val username = message.from?.username
        val text = when {
            groupName.isNullOrEmpty() -> MessageProvider.noGroupName()
            username.isNullOrEmpty() -> MessageProvider.noUsername()
            subGroupsManager.getSubGroup(message.chat.id, groupName) == null ->
                MessageProvider.groupNotFound(groupName)
            subGroupsManager.removeMember(groupName, message.chat.id, username) ->
                MessageProvider.userLeftGroup(username, groupName)
            else -> MessageProvider.userNotInGroup(username, groupName)
        }
        conversationContext.sendMessage(text, lifetime = LivingMessage.MessageLifetime.LONG)
    }

    private fun renameSubGroup(oldGroupName: String?, newGroupName: String?, message: Message) {
        val text =
            if (oldGroupName.isNullOrEmpty() || newGroupName.isNullOrEmpty()) {
                MessageProvider.noGroupName()
            }
            else {
                val group = subGroupsManager.getSubGroup(message.chat.id, oldGroupName)
                when {
                    (group == null) -> MessageProvider.groupNotFound(oldGroupName)
                    !group.admins.contains(message.from?.id) -> {
                        val adminUsernames = group.admins.map {
                            JanaBot.bot.getChatMember(message.chat, it.toLong()).get().user.username
                        }
                        MessageProvider.noPrivileges(oldGroupName, adminUsernames)
                    }
                    subGroupsManager.getSubGroup(message.chat.id, newGroupName) != null -> MessageProvider.groupExists(newGroupName)
                    subGroupsManager.renameSubGroup(oldGroupName, newGroupName, message.chat.id) -> MessageProvider.groupRenamed(oldGroupName, newGroupName)
                    else -> {
                        logger.warn { "Rename method failed, this should not happen" }
                        MessageProvider.groupNotFound(oldGroupName)
                    }
                }
            }
        conversationContext.sendMessage(text, lifetime = LivingMessage.MessageLifetime.LONG)

    }

    private fun deleteSubGroup(groupName: String?, message: Message) {
        val user = message.from
        val text = if (groupName.isNullOrEmpty()) MessageProvider.noGroupName()
        else if (user?.username == null) MessageProvider.noUsername()
        else {
            val group = subGroupsManager.getSubGroup(message.chat.id, groupName)
            if (group == null) MessageProvider.groupNotFound(groupName)
            else if (group.admins.isEmpty() || !group.admins.contains(user.id)) {
                val adminUsernames = group.admins.map {
                    JanaBot.bot.getChatMember(message.chat, it.toLong()).get().user.username
                }
                MessageProvider.noPrivileges(groupName, adminUsernames)
            } else if (subGroupsManager.deleteSubGroup(groupName, message.chat.id)) {
                MessageProvider.groupDeleted(groupName)
            } else {
                logger.warn { "Group not found within delete method, this should not happen" }
                MessageProvider.groupNotFound(groupName)
            }
        }
        conversationContext.sendMessage(text, lifetime = LivingMessage.MessageLifetime.LONG)
    }

    private fun getSubGroupMembers(groupName: String?, message: Message) {
        val text = if (groupName.isNullOrEmpty()) MessageProvider.noGroupName()
        else {
            val members = subGroupsManager.getMembersList(groupName, message.chat.id)
            when {
                members == null -> MessageProvider.groupNotFound(groupName)
                members.isEmpty() -> MessageProvider.noGroupMembers(groupName)
                else -> MessageProvider.subGroupString(groupName, members)
            }
        }
        conversationContext.sendMessage(text, lifetime = LivingMessage.MessageLifetime.SHORT)
    }

    private fun getChatSubgroups(message: Message) {
        val subGroups = subGroupsManager.getChatSubGroups(message.chat.id)
        val text = if (subGroups.isEmpty()) MessageProvider.noSubGroups()
        else {
            val subGroupsString = StringBuilder()
            subGroups.forEach {
                if (it.members.isEmpty()) {
                    subGroupsString.appendLine(MessageProvider.noGroupMembers(it.name))
                } else {
                    subGroupsString
                        .appendLine(MessageProvider.subGroupString(it.name, it.members))
                }
            }
            MessageProvider.chatSubGroups(subGroupsString.toString())
        }
        conversationContext.sendMessage(text, lifetime = LivingMessage.MessageLifetime.SHORT)
    }

    private fun getAllSubGroups(message: Message) {
        val groups = subGroupsManager.getAllGroups()
        val text = if (groups.isNotEmpty()) {
            "Current groups across all chats are: \n${groups.joinToString("\n")}"
        } else {
            "No groups currently exist anywhere."
        }
        conversationContext.sendMessage(text, lifetime = LivingMessage.MessageLifetime.SHORT)
    }

    // TODO add possibility to load a backup by passing a parameter
    private fun loadSubGroups() {
        subGroupsManager.load()
    }

    private fun backupSubGroups() {
        subGroupsManager.saveBackup()
    }

    private fun getAvailableBackups(message: Message) {
        subGroupsManager.getBackups(AsyncResultListener(message.chat.id, message.message_id))
    }

    object MessageProvider {
        fun noCommand(): String {
            return "No /group argument (use /help if needed)"
        }

        fun unrecognizedArgument(usedCommand: String): String {
            return "Unrecognized group command \"$usedCommand\" (use /help if needed)"
        }

        fun notEnoughArguments(usedCommand: String): String {
            return "Given command probably requires more arguments: \"$usedCommand\" (use /help if needed)"
        }

        fun noGroupName(): String {
            return "No group name provided!"
        }

        fun noUsernameProvided(): String {
            return "No username provided!"
        }

        fun groupCreated(groupName: String, userName: String?): String {
            return if (userName != null) {
                "User $userName created group \"$groupName\"."
            } else {
                "You have created group: \"$groupName\"."
            }
        }

        fun groupExists(groupName: String): String {
            return "Group with name \"$groupName\" already exists."
        }

        fun groupNotFound(groupName: String): String {
            return "Group with name \"$groupName\" doesn't exist.\n" +
                    "Create groups using /group -create <name>."
        }

        fun noUsername(): String {
            return "Hey, you don't have a username!\n" +
                    "Please, set your username in the settings and retry joining, otherwise I can't tag you properly :(."
        }

        fun userAddedToGroup(username: String, groupName: String): String {
            return "User $username has been added to the group \"$groupName\"."
        }

        fun userInGroup(username: String, groupName: String): String {
            return "User $username is already in group \"$groupName\""
        }

        fun userLeftGroup(username: String, groupName: String): String {
            return "User with username $username left the group \"$groupName\"."
        }

        fun userRemovedFromGroupBy(username: String, removedBy: String, groupName: String): String {
            return "User with username $username was removed from the group \"$groupName\" by $removedBy."
        }

        fun userNotInGroup(username: String, groupName: String): String {
            return "$username is not a member of the group \"$groupName\"."
        }

        fun groupDeleted(groupName: String): String {
            return "Group \"$groupName\" deleted."
        }

        fun groupRenamed(oldGroupName: String, newGroupName: String): String {
            return "Group \"$oldGroupName\" renamed to \"$newGroupName\"."
        }

        fun noPrivileges(groupName: String, adminUsernames: List<String?>): String {
            return "This action can only be performed by the group creator!\n" +
                    "Group \"$groupName\" admins are: $adminUsernames."
        }

        fun noGroupMembers(groupName: String): String {
            return "Group \"$groupName\" has no members!\n" +
                    "People can join using \"/group -join $groupName\" command."
        }

        fun subGroupString(groupName: String, members: List<String>): String {
            return "Group \"$groupName\" members are: $members"
        }

        fun noSubGroups(): String {
            return "No groups currently exist in this chat."
        }

        fun chatSubGroups(groups: String): String {
            return "Current groups in this chat are:\n$groups"
        }

        fun repositoryOperationResult(operation: String, isSuccess: Boolean, data: List<String> = emptyList()): String {
            val resultString = if (isSuccess) "success" else "failure"
            return "Result of $operation is $resultString. $data"
        }
    }

    class AsyncResultListener(private val chatID: Long, private val triggerer: Int?): OperationResultListener {
        override fun onOperationDone(operationName: String, isSuccess: Boolean, data: List<String>) {
            // can't use context here since it's asynchronous and the variables are passed through
            JanaBot.bot.sendMessage(chatID, MessageProvider.repositoryOperationResult(operationName, isSuccess, data),
                replyTo = triggerer, lifetime = LivingMessage.MessageLifetime.MEDIUM)
        }
    }
}
