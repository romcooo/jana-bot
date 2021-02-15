package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import com.elbekD.bot.types.User
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.utils.getAdminId

class GroupCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/group") {

    override suspend fun onCommand(message: Message, s: String?) {
        val conversation = Conversation(message)
        val user = message.from
        val args = message.text?.split(" ")?.drop(1)
        logger.debug { "Executing command: $args" }
        if (args == null || args.isEmpty()) {
            conversation.sendMessage(JanaBot.messages.get("group.noCommand"))
            conversation.burnConversation(MessageLifetime.SHORT)
            return
        }
        when (args[0].toLowerCase()) {
            "-create" -> createSubGroup(args, conversation, user)
            "-join" -> joinSubGroup(args, conversation, user)
            "-leave" -> leaveSubGroup(args, conversation, user)
            "-kick" -> removeMember(args, conversation, user)
            "-add" -> addMember(args, conversation, user)
            "-delete" -> deleteSubGroup(args, conversation, user)
            "-members" -> getSubGroupMembers(args, conversation)
            "-list" -> getChatSubgroups(conversation)
            "-listAll".toLowerCase() -> getAllSubGroups(conversation)
            "-rename" -> renameSubGroup(args, conversation, user)
            "-help" -> {
                conversation.sendMessage(help())
                conversation.burnConversation(MessageLifetime.SHORT)
            }
            else -> {
                logger.debug { "Unknown argument used: ${args[0]}" }
                conversation.sendMessage(text = JanaBot.messages.get("group.unknownCommand", args[1]))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
        }
    }

    private suspend fun createSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            else -> if (subGroupsManager.createSubGroup(groupName, conversation.chatId, user?.id)) {
                JanaBot.messages.get("group.created", groupName)
            } else {
                JanaBot.messages.get("group.nameExists", groupName)
            }
        }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun joinSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val username = user?.username
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            username.isNullOrEmpty() -> JanaBot.messages.get("group.dontHaveUsername")
            subGroupsManager.getSubGroup(conversation.chatId, groupName) == null ->
                JanaBot.messages.get("group.notFound", groupName)
            subGroupsManager.addMember(groupName, conversation.chatId, username) ->
                JanaBot.messages.get("group.userAdded", username, groupName)
            else -> JanaBot.messages.get("group.userAlreadyIn", username, groupName)
        }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.MEDIUM)
    }

    private suspend fun addMember(args: List<String>, conversation: Conversation, user: User?) {
        val callerUsername = user?.username
        val groupName = args.getArg(1)
        val usernameToAdd = args.getArg(2)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            callerUsername.isNullOrEmpty() -> JanaBot.messages.get("group.dontHaveUsername")
            usernameToAdd.isNullOrEmpty() -> JanaBot.messages.get("group.noUsername")
            subGroupsManager.getSubGroup(conversation.chatId, groupName) == null ->
                JanaBot.messages.get("group.notFound", groupName)
            JanaBot.bot.getAdminId(conversation.chatId, usernameToAdd) == null ->
                JanaBot.messages.get("group.notAnAdmin")
            subGroupsManager.addMember(groupName, conversation.chatId, usernameToAdd) ->
                JanaBot.messages.get("group.userAdded", usernameToAdd, groupName)
            else -> JanaBot.messages.get("group.userAlreadyIn", usernameToAdd, groupName)
        }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun removeMember(args: List<String>, conversation: Conversation, user: User?) {
        val callerUsername = user?.username
        val groupName = args.getArg(1)
        val usernameToRemove = args.getArg(2)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            callerUsername.isNullOrEmpty() -> JanaBot.messages.get("group.dontHaveUsername")
            usernameToRemove.isNullOrEmpty() -> JanaBot.messages.get("group.noUsername")
            else -> {
                val group = subGroupsManager.getSubGroup(conversation.chatId, groupName)
                if (group == null) {
                    JanaBot.messages.get("group.notFound", groupName)
                } else if (!group.admins.contains(user.id)) {
                    val adminUsernames = group.admins.map {
                        JanaBot.bot.getChatMember(conversation.chatId, it.toLong()).get().user.username
                    }
                    JanaBot.messages.get("group.noPrivileges", groupName, adminUsernames.toString())
                } else if (subGroupsManager.removeMember(groupName, conversation.chatId, usernameToRemove)) {
                    JanaBot.messages.get("group.userRemoved", usernameToRemove, groupName, callerUsername)
                } else {
                    JanaBot.messages.get("group.userNotIn", usernameToRemove, groupName)
                }
            }
        }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun leaveSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val username = user?.username
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            username.isNullOrEmpty() -> JanaBot.messages.get("group.dontHaveUsername")
            subGroupsManager.getSubGroup(conversation.chatId, groupName) == null ->
                JanaBot.messages.get("group.notFound", groupName)
            subGroupsManager.removeMember(groupName, conversation.chatId, username) ->
                JanaBot.messages.get("group.userLeft", username, groupName)
            else -> JanaBot.messages.get("group.userNotIn", username, groupName)
        }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.MEDIUM)
    }

    private suspend fun renameSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val oldGroupName = args.getArg(1)
        val newGroupName = args.getArg(2)
        val text =
            if (oldGroupName.isNullOrEmpty() || newGroupName.isNullOrEmpty()) {
                JanaBot.messages.get("group.noName")
            } else {
                val group = subGroupsManager.getSubGroup(conversation.chatId, oldGroupName)
                when {
                    (group == null) -> JanaBot.messages.get("group.notFound", oldGroupName)
                    !group.admins.contains(user?.id) -> {
                        val adminUsernames = group.admins.map {
                            JanaBot.bot.getChatMember(conversation.chatId, it.toLong()).get().user.username
                        }
                        JanaBot.messages.get("group.noPrivileges", oldGroupName, adminUsernames.toString())
                    }
                    subGroupsManager.getSubGroup(conversation.chatId, newGroupName) != null ->
                        JanaBot.messages.get("group.nameExists", newGroupName)
                    subGroupsManager.renameSubGroup(oldGroupName, newGroupName, conversation.chatId) ->
                        JanaBot.messages.get("group.renamed", oldGroupName, newGroupName)
                    else -> {
                        logger.warn { "Rename method failed, this should not happen" }
                        JanaBot.messages.get("group.notFound", oldGroupName)
                    }
                }
            }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun deleteSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            user?.username == null -> JanaBot.messages.get("group.dontHaveUsername")
            else -> {
                val group = subGroupsManager.getSubGroup(conversation.chatId, groupName)
                if (group == null) JanaBot.messages.get("group.notFound", groupName)
                else if (group.admins.isEmpty() || !group.admins.contains(user.id)) {
                    val adminUsernames = group.admins.map {
                        JanaBot.bot.getChatMember(conversation.chatId, it.toLong()).get().user.username
                    }
                    JanaBot.messages.get("group.noPrivileges", groupName, adminUsernames.toString())
                } else if (subGroupsManager.deleteSubGroup(groupName, conversation.chatId)) {
                    JanaBot.messages.get("group.deleted", groupName)
                } else {
                    logger.warn { "Group not found within delete method, this should not happen" }
                    JanaBot.messages.get("group.notFound", groupName)
                }
            }
        }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun getSubGroupMembers(args: List<String>, conversation: Conversation) {
        val groupName = args.getArg(1)
        val text = if (groupName.isNullOrEmpty()) JanaBot.messages.get("group.noName")
        else {
            val members = subGroupsManager.getMembersList(groupName, conversation.chatId)
            when {
                members == null -> JanaBot.messages.get("group.notFound", groupName)
                members.isEmpty() -> JanaBot.messages.get("group.noMembers", groupName)
                else -> JanaBot.messages.get("group.printGroup", groupName, members.toString())
            }
        }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    private suspend fun getChatSubgroups(conversation: Conversation) {
        val subGroups = subGroupsManager.getChatSubGroups(conversation.chatId)
        val text = if (subGroups.isEmpty()) JanaBot.messages.get("group.noGroups")
        else {
            val subGroupsString = StringBuilder()
            subGroups.forEach {
                if (it.members.isEmpty()) {
                    subGroupsString.appendLine(JanaBot.messages.get("group.noMembers", it.name))
                } else {
                    subGroupsString.appendLine(JanaBot.messages.get("group.printGroup", it.name, it.members.toString()))
                }
            }
            JanaBot.messages.get("group.chatGroups", subGroupsString.toString())
        }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    private suspend fun getAllSubGroups(conversation: Conversation) {
        val groups = subGroupsManager.getAllGroups()
        val text = if (groups.isNotEmpty()) {
            JanaBot.messages.get("group.allGroups", groups.joinToString("\n"))
        } else {
            JanaBot.messages.get("group.noneExist")
        }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    override fun help(): String {
        logger.trace { "SubGroup help called" }
        return JanaBot.messages.get("group.help")
    }

    private fun List<String>.getArg(index: Int): String? {
        return if (this.size < index + 1) null else this[index]
    }
}
