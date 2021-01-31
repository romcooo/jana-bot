package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.LivingMessage
import org.koppakurhiev.janabot.persistence.Repository.OperationResultListener
import org.koppakurhiev.janabot.sendMessageWithLifetime
import org.koppakurhiev.janabot.services.ABotService

class GroupCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/group") {

    override fun onCommand(message: Message, s: String?) {
        val words = message.text?.split(" ")
        logger.debug { "Executing command: $words" }
        if (words == null || words.size < 2) {
            JanaBot.bot.sendMessageWithLifetime(message.chat.id, MessageProvider.noCommand())
            return
        }
        val targetGroupName: String? = if (words.size >= 3) words[2] else null
        when (words[1]) {
            "-create" -> createSubGroup(targetGroupName, message)
            "-join" -> joinSubGroup(targetGroupName, message)
            "-leave" -> leaveSubGroup(targetGroupName, message)
            "-delete" -> deleteSubGroup(targetGroupName, message)
            "-members" -> getSubGroupMembers(targetGroupName, message)
            "-list" -> getChatSubgroups(message)
            // Use for testing purposes TODO - consider deleting later (but it is kind of useful)
            "-listAll" -> getAllSubGroups(message)
            "-saveBackup" -> backupSubGroups()
            "-listBackups" -> getAvailableBackups(message)
            // TODO allow loading a backup directly
//            "-load" -> loadSubGroups(filename)
            else -> {
                logger.trace { "Unknown argument used: ${words[1]}" }
                JanaBot.bot.sendMessageWithLifetime(message.chat.id, MessageProvider.unrecognizedArgument(words[1]))
            }
        }

        JanaBot.messageCleaner.registerMessage(
            LivingMessage(chatId = message.chat.id, messageId = message.message_id, lifetime = LivingMessage.MessageLifetime.MEDIUM))
    }

    private fun createSubGroup(groupName: String?, message: Message) {
        val text = if (groupName == null || groupName.isEmpty()) {
            MessageProvider.noGroupName()
        } else {
            if (subGroupsManager.createSubGroup(groupName, message.chat.id, message.from?.id)) {
                MessageProvider.groupCreated(groupName, message.from?.first_name)
            } else {
                MessageProvider.groupExists(groupName)
            }
        }
        JanaBot.bot.sendMessageWithLifetime(message.chat.id, text)
    }

    private fun joinSubGroup(groupName: String?, message: Message) {
        val username = message.from?.username
        val text = when {
            groupName == null || groupName.isEmpty() -> MessageProvider.noGroupName()
            username == null -> MessageProvider.noUsername()
            subGroupsManager.getSubGroup(message.chat.id, groupName) == null ->
                MessageProvider.groupNotFound(groupName)
            subGroupsManager.addMember(groupName, message.chat.id, username) ->
                MessageProvider.userAddedToGroup(username, groupName)
            else -> MessageProvider.userInGroup(username, groupName)
        }
        JanaBot.bot.sendMessageWithLifetime(message.chat.id, text)
    }

    private fun leaveSubGroup(groupName: String?, message: Message) {
        val username = message.from?.username
        val text = when {
            groupName == null || groupName.isEmpty() -> MessageProvider.noGroupName()
            username == null -> MessageProvider.noUsername()
            subGroupsManager.getSubGroup(message.chat.id, groupName) == null ->
                MessageProvider.groupNotFound(groupName)
            subGroupsManager.removeMember(groupName, message.chat.id, username) ->
                MessageProvider.userLeftGroup(username, groupName)
            else -> MessageProvider.userNotInGroup(username, groupName)
        }
        JanaBot.bot.sendMessageWithLifetime(message.chat.id, text)
    }

    private fun deleteSubGroup(groupName: String?, message: Message) {
        val user = message.from
        val text = if (groupName == null || groupName.isEmpty()) MessageProvider.noGroupName()
        else if (user?.username == null) MessageProvider.noUsername()
        else {
            val group = subGroupsManager.getSubGroup(message.chat.id, groupName)
            if (group == null) MessageProvider.groupNotFound(groupName)
            else if (group.creatorId != -1 && group.creatorId != user.id) {
                val adminUsername =
                    JanaBot.bot.getChatMember(message.chat, group.creatorId.toLong()).get().user.username
                MessageProvider.noPrivileges(groupName, adminUsername)
            } else if (subGroupsManager.deleteSubGroup(groupName, message.chat.id)) {
                MessageProvider.groupDeleted(groupName)
            } else {
                logger.warn { "Group not found within delete method, this should not happen" }
                MessageProvider.groupNotFound(groupName)
            }
        }
        JanaBot.bot.sendMessageWithLifetime(message.chat.id, text)
    }

    private fun getSubGroupMembers(groupName: String?, message: Message) {
        val text = if (groupName == null || groupName.isEmpty()) MessageProvider.noGroupName()
        else {
            val members = subGroupsManager.getMembersList(groupName, message.chat.id)
            when {
                members == null -> MessageProvider.groupNotFound(groupName)
                members.isEmpty() -> MessageProvider.noGroupMembers(groupName)
                else -> MessageProvider.subGroupString(groupName, members)
            }
        }
        JanaBot.bot.sendMessageWithLifetime(message.chat.id, text)
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
        JanaBot.bot.sendMessageWithLifetime(message.chat.id, text)
    }

    private fun getAllSubGroups(message: Message) {
        val groups = subGroupsManager.getAllGroups()
        val text = if (groups.isNotEmpty()) {
            "Current groups across all chats are: \n${groups.joinToString("\n")}"
        } else {
            "No groups currently exist anywhere."
        }
        JanaBot.bot.sendMessageWithLifetime(message.chat.id, text)
    }

    // TODO add possibility to load a backup by passing a parameter
    private fun loadSubGroups() {
        subGroupsManager.load()
    }

    private fun backupSubGroups() {
        subGroupsManager.saveBackup()
    }

    private fun getAvailableBackups(message: Message) {
        subGroupsManager.getBackups(AsyncResultListener(message.chat.id))
    }

    object MessageProvider {
        fun noCommand(): String {
            return "No /group argument (use /help if needed)"
        }

        fun unrecognizedArgument(usedCommand: String): String {
            return "Unrecognized group command \"$usedCommand\" (use /help if needed)"
        }

        fun noGroupName(): String {
            return "No group name provided!"
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
            return "User $username has been added to the group \"$groupName\""
        }

        fun userInGroup(username: String, groupName: String): String {
            return "User $username is already in group \"$groupName\""
        }

        fun userLeftGroup(username: String, groupName: String): String {
            return "User with username $username left the group \"$groupName\"."
        }

        fun userNotInGroup(username: String, groupName: String): String {
            return "$username is not a member of the group \"$groupName\"."
        }

        fun groupDeleted(groupName: String): String {
            return "Group \"$groupName\" deleted."
        }

        fun noPrivileges(groupName: String, adminUsername: String?): String {
            return "This action can only be performed by the group creator!\n" +
                    "Group \"$groupName\" was created by user $adminUsername."
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

        fun repositoryOperationResult(operation: String, isSuccess: Boolean): String {
            val resultString = if (isSuccess) "success" else "failure"
            return "Result of $operation is $resultString"
        }
    }

    class AsyncResultListener(private val chatID: Long): OperationResultListener {
        override fun onOperationDone(operationName: String, isSuccess: Boolean) {
            JanaBot.bot.sendMessageWithLifetime(chatID, MessageProvider.repositoryOperationResult(operationName, isSuccess))
        }
    }
}
