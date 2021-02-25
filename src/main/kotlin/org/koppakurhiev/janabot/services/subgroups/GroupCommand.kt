package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.User
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.utils.getAdminId
import org.koppakurhiev.janabot.utils.getArg

class GroupCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/group") {

    override suspend fun onCommand(message: Message, s: String?) {
        val conversation = Conversation(message)
        val user = message.from
        val args = message.text?.split(" ")?.drop(1)
        logger.debug { "Executing command: $args" }
        if (args == null || args.isEmpty()) {
            conversation.replyMessage(JanaBot.messages.get("group.noCommand"))
            conversation.burnConversation(MessageLifetime.FLASH)
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
            "-admins" -> getSubGroupAdmins(args, conversation)
            "-list" -> getChatSubgroups(conversation)
            "-listAll".toLowerCase() -> getAllSubGroups(conversation)
            "-rename" -> renameSubGroup(args, conversation, user)
            "-help" -> {
                conversation.replyMessage(help(message))
                conversation.burnConversation(MessageLifetime.SHORT)
            }
            else -> {
                logger.debug { "Unknown argument used: ${args[0]}" }
                conversation.replyMessage(text = JanaBot.messages.get("unknownCommand", args[0]))
                conversation.burnConversation(MessageLifetime.FLASH)
            }
        }
    }

    private suspend fun createSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            else -> {
                val operationResult = subGroupsManager.createSubGroup(groupName, conversation.chatId, user?.id)
                standardReply(operationResult, JanaBot.messages.get("group.created", groupName))
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun joinSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val username = user?.username
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            username.isNullOrEmpty() -> JanaBot.messages.get("group.dontHaveUsername")
            else -> {
                val operationResult = subGroupsManager.addMember(groupName, conversation.chatId, username)
                standardReply(operationResult, JanaBot.messages.get("group.userAdded", username, groupName))
            }
        }
        conversation.replyMessage(text)
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
            JanaBot.bot.getAdminId(conversation.chatId, usernameToAdd) == null ->
                JanaBot.messages.get("group.notAnAdmin")
            else -> {
                var operationResult = subGroupsManager.isGroupAdmin(conversation.chatId, groupName, user.id)
                if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                    operationResult = subGroupsManager.addMember(groupName, conversation.chatId, usernameToAdd)
                }
                standardReply(operationResult, JanaBot.messages.get("group.userAdded", usernameToAdd, groupName))
            }
        }
        conversation.replyMessage(text)
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
                var operationResult = subGroupsManager.isGroupAdmin(conversation.chatId, groupName, user.id)
                if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                    operationResult = subGroupsManager.removeMember(groupName, conversation.chatId, usernameToRemove)
                }
                standardReply(operationResult, JanaBot.messages.get("group.userRemoved", usernameToRemove, groupName))
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun leaveSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val username = user?.username
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            username.isNullOrEmpty() -> JanaBot.messages.get("group.dontHaveUsername")
            else -> {
                val operationResult = subGroupsManager.removeMember(groupName, conversation.chatId, username)
                standardReply(operationResult, JanaBot.messages.get("group.userLeft", username, groupName))
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.MEDIUM)
    }

    private suspend fun renameSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val oldGroupName = args.getArg(1)
        val newGroupName = args.getArg(2)
        val text = when {
            user == null -> JanaBot.messages.get("group.noName")
            (oldGroupName.isNullOrEmpty() || newGroupName.isNullOrEmpty()) -> JanaBot.messages.get("group.noName")
            else -> {
                var operationResult = subGroupsManager.isGroupAdmin(conversation.chatId, oldGroupName, user.id)
                if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                    operationResult = subGroupsManager.renameSubGroup(oldGroupName, newGroupName, conversation.chatId)
                }
                standardReply(operationResult, JanaBot.messages.get("group.renamed", oldGroupName, newGroupName))
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun deleteSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            user == null -> JanaBot.messages.get("group.noName")
            else -> {
                var operationResult = subGroupsManager.isGroupAdmin(conversation.chatId, groupName, user.id)
                if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                    operationResult = subGroupsManager.deleteSubGroup(groupName, conversation.chatId)
                }
                standardReply(operationResult, JanaBot.messages.get("group.deleted", groupName))
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun getSubGroupMembers(args: List<String>, conversation: Conversation) {
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            else -> {
                val members = subGroupsManager.getMembersList(groupName, conversation.chatId)
                when {
                    members == null -> JanaBot.messages.get("group.notFound", groupName)
                    members.isEmpty() -> JanaBot.messages.get("group.noMembers", groupName)
                    else -> JanaBot.messages.get("group.printGroup", groupName, members.joinToString())
                }
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    private suspend fun getSubGroupAdmins(args: List<String>, conversation: Conversation) {
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            else -> {
                val admins = subGroupsManager.getAdmins(conversation.chatId, groupName)
                when {
                    admins == null -> JanaBot.messages.get("group.notFound")
                    admins.isEmpty() -> JanaBot.messages.get("group.noAdmins")
                    else -> {
                        val adminsStr = admins.map {
                            JanaBot.bot.getChatMember(conversation.chatId, it.toLong()).await().user.username
                        }
                        JanaBot.messages.get("group.adminList", adminsStr.joinToString())
                    }
                }
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    private suspend fun getChatSubgroups(conversation: Conversation) {
        val subGroups = subGroupsManager.getChatSubGroups(conversation.chatId)
        val text = when {
            subGroups.isEmpty() -> JanaBot.messages.get("group.noGroups")
            else -> {
                val subGroupsString = StringBuilder()
                subGroups.forEach {
                    if (it.members.isEmpty()) {
                        subGroupsString.appendLine(JanaBot.messages.get("group.noMembers", it.name))
                    } else {
                        subGroupsString.appendLine(
                            JanaBot.messages.get(
                                "group.printGroup",
                                it.name,
                                it.members.joinToString()
                            )
                        )
                    }
                }
                JanaBot.messages.get("group.chatGroups", subGroupsString.toString())
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    private suspend fun getAllSubGroups(conversation: Conversation) {
        val groups = subGroupsManager.getAllGroups()
        val text = if (groups.isNotEmpty()) {
            JanaBot.messages.get("group.allGroups", groups.joinToString("\n"))
        } else {
            JanaBot.messages.get("group.noneExist")
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    override fun help(message: Message?): String {
        logger.trace { "SubGroup help called" }
        return JanaBot.messages.get("group.help")
    }

    private fun standardReply(operationResult: SubGroupsManager.OperationResult, onSuccess: String): String {
        return when (operationResult) {
            SubGroupsManager.OperationResult.GROUP_NOT_FOUND -> JanaBot.messages.get("group.notFound")
            SubGroupsManager.OperationResult.GROUP_ALREADY_EXISTS -> JanaBot.messages.get("group.nameExists")
            SubGroupsManager.OperationResult.GROUP_MISSING_MEMBER -> JanaBot.messages.get("group.missing")
            SubGroupsManager.OperationResult.GROUP_CONTAINS_MEMBER -> JanaBot.messages.get("group.userAlreadyIn")
            SubGroupsManager.OperationResult.NOT_GROUP_ADMIN -> JanaBot.messages.get("group.noPrivileges")
            SubGroupsManager.OperationResult.LOAD_FAILED -> JanaBot.messages.get("db.loadFailed")
            SubGroupsManager.OperationResult.SAVE_FAILED -> JanaBot.messages.get("db.saveFailed")
            SubGroupsManager.OperationResult.UNKNOWN_ERROR -> JanaBot.messages.get("unknownError")
            SubGroupsManager.OperationResult.SUCCESS -> onSuccess
        }
    }
}
