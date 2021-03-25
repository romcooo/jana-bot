package org.koppakurhiev.janabot.telegram.services.subgroups

import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.User
import org.koppakurhiev.janabot.common.CommonStrings
import org.koppakurhiev.janabot.common.getArg
import org.koppakurhiev.janabot.telegram.TelegramStrings
import org.koppakurhiev.janabot.telegram.bot.Conversation
import org.koppakurhiev.janabot.telegram.bot.MessageLifetime
import org.koppakurhiev.janabot.telegram.bot.getAdminId
import org.koppakurhiev.janabot.telegram.services.ABotService

class GroupCommand(service: SubGroupsService) :
    ABotService.ABotCommand(service, "/group") {

    private val subGroupsManager = service.subGroupsManager

    override suspend fun onCommand(message: Message, s: String?) {
        val conversation = Conversation(service.bot, message)
        val user = message.from
        val args = message.text?.split(" ")?.drop(1)
        logger.debug { "Executing command: $args" }
        if (args == null || args.isEmpty()) {
            conversation.replyMessage(TelegramStrings.getString("group.noCommand"))
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
                conversation.replyMessage(CommonStrings.getString("unknownCommand", args[0]))
                conversation.burnConversation(MessageLifetime.FLASH)
            }
        }
    }

    private suspend fun createSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> TelegramStrings.getString("group.noName")
            else -> {
                val operationResult = subGroupsManager.createSubGroup(groupName, conversation.chatId, user?.id)
                standardReply(operationResult, TelegramStrings.getString("group.created", groupName))
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun joinSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val username = user?.username
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> TelegramStrings.getString("group.noName")
            username.isNullOrEmpty() -> TelegramStrings.getString("group.dontHaveUsername")
            else -> {
                val operationResult = subGroupsManager.addMember(groupName, conversation.chatId, username)
                standardReply(operationResult, TelegramStrings.getString("group.userAdded", username, groupName))
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
            groupName.isNullOrEmpty() -> TelegramStrings.getString("group.noName")
            callerUsername.isNullOrEmpty() -> TelegramStrings.getString("group.dontHaveUsername")
            usernameToAdd.isNullOrEmpty() -> TelegramStrings.getString("group.noUsername")
            service.bot.getBot().getAdminId(conversation.chatId, usernameToAdd) == null ->
                TelegramStrings.getString("group.notAnAdmin")
            else -> {
                var operationResult = subGroupsManager.isGroupAdmin(conversation.chatId, groupName, user.id)
                if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                    operationResult = subGroupsManager.addMember(groupName, conversation.chatId, usernameToAdd)
                }
                standardReply(operationResult, TelegramStrings.getString("group.userAdded", usernameToAdd, groupName))
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
            groupName.isNullOrEmpty() -> TelegramStrings.getString("group.noName")
            callerUsername.isNullOrEmpty() -> TelegramStrings.getString("group.dontHaveUsername")
            usernameToRemove.isNullOrEmpty() -> TelegramStrings.getString("group.noUsername")
            else -> {
                var operationResult = subGroupsManager.isGroupAdmin(conversation.chatId, groupName, user.id)
                if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                    operationResult = subGroupsManager.removeMember(groupName, conversation.chatId, usernameToRemove)
                }
                standardReply(
                    operationResult,
                    TelegramStrings.getString("group.userRemoved", usernameToRemove, groupName)
                )
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun leaveSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val username = user?.username
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> TelegramStrings.getString("group.noName")
            username.isNullOrEmpty() -> TelegramStrings.getString("group.dontHaveUsername")
            else -> {
                val operationResult = subGroupsManager.removeMember(groupName, conversation.chatId, username)
                standardReply(operationResult, TelegramStrings.getString("group.userLeft", username, groupName))
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.MEDIUM)
    }

    private suspend fun renameSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val oldGroupName = args.getArg(1)
        val newGroupName = args.getArg(2)
        val text = when {
            user == null -> TelegramStrings.getString("group.noName")
            (oldGroupName.isNullOrEmpty() || newGroupName.isNullOrEmpty()) -> TelegramStrings.getString("group.noName")
            else -> {
                var operationResult = subGroupsManager.isGroupAdmin(conversation.chatId, oldGroupName, user.id)
                if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                    operationResult = subGroupsManager.renameSubGroup(oldGroupName, newGroupName, conversation.chatId)
                }
                standardReply(operationResult, TelegramStrings.getString("group.renamed", oldGroupName, newGroupName))
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun deleteSubGroup(args: List<String>, conversation: Conversation, user: User?) {
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> TelegramStrings.getString("group.noName")
            user == null -> TelegramStrings.getString("group.noName")
            else -> {
                var operationResult = subGroupsManager.isGroupAdmin(conversation.chatId, groupName, user.id)
                if (operationResult == SubGroupsManager.OperationResult.SUCCESS) {
                    operationResult = subGroupsManager.deleteSubGroup(groupName, conversation.chatId)
                }
                standardReply(operationResult, TelegramStrings.getString("group.deleted", groupName))
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.LONG)
    }

    private suspend fun getSubGroupMembers(args: List<String>, conversation: Conversation) {
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> TelegramStrings.getString("group.noName")
            else -> {
                val members = subGroupsManager.getMembersList(groupName, conversation.chatId)
                when {
                    members == null -> TelegramStrings.getString("group.notFound", groupName)
                    members.isEmpty() -> TelegramStrings.getString("group.noMembers", groupName)
                    else -> TelegramStrings.getString("group.printGroup", groupName, members.joinToString())
                }
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    private suspend fun getSubGroupAdmins(args: List<String>, conversation: Conversation) {
        val groupName = args.getArg(1)
        val text = when {
            groupName.isNullOrEmpty() -> TelegramStrings.getString("group.noName")
            else -> {
                val admins = subGroupsManager.getAdmins(conversation.chatId, groupName)
                when {
                    admins == null -> TelegramStrings.getString("group.notFound")
                    admins.isEmpty() -> TelegramStrings.getString("group.noAdmins")
                    else -> {
                        val adminsStr = admins.map {
                            service.bot.getBot().getChatMember(conversation.chatId, it.toLong()).await().user.username
                        }
                        TelegramStrings.getString("group.adminList", adminsStr.joinToString())
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
            subGroups.isEmpty() -> TelegramStrings.getString("group.noGroups")
            else -> {
                val subGroupsString = StringBuilder()
                subGroups.forEach {
                    if (it.members.isEmpty()) {
                        subGroupsString.appendLine(TelegramStrings.getString("group.noMembers", it.name))
                    } else {
                        subGroupsString.appendLine(
                            TelegramStrings.getString(
                                "group.printGroup",
                                it.name,
                                it.members.joinToString()
                            )
                        )
                    }
                }
                TelegramStrings.getString("group.chatGroups", subGroupsString.toString())
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    private suspend fun getAllSubGroups(conversation: Conversation) {
        val groups = subGroupsManager.getAllGroups()
        val text = if (groups.isNotEmpty()) {
            TelegramStrings.getString("group.allGroups", groups.joinToString("\n"))
        } else {
            TelegramStrings.getString("group.noneExist")
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    override fun help(message: Message?): String {
        logger.trace { "SubGroup help called" }
        return TelegramStrings.getString("group.help")
    }

    private fun standardReply(operationResult: SubGroupsManager.OperationResult, onSuccess: String): String {
        return when (operationResult) {
            SubGroupsManager.OperationResult.GROUP_NOT_FOUND -> TelegramStrings.getString("group.notFound")
            SubGroupsManager.OperationResult.GROUP_ALREADY_EXISTS -> TelegramStrings.getString("group.nameExists")
            SubGroupsManager.OperationResult.GROUP_MISSING_MEMBER -> TelegramStrings.getString("group.missing")
            SubGroupsManager.OperationResult.GROUP_CONTAINS_MEMBER -> TelegramStrings.getString("group.userAlreadyIn")
            SubGroupsManager.OperationResult.NOT_GROUP_ADMIN -> TelegramStrings.getString("group.noPrivileges")
            SubGroupsManager.OperationResult.LOAD_FAILED -> CommonStrings.getString("db.loadFailed")
            SubGroupsManager.OperationResult.SAVE_FAILED -> CommonStrings.getString("db.saveFailed")
            SubGroupsManager.OperationResult.UNKNOWN_ERROR -> CommonStrings.getString("unknownError")
            SubGroupsManager.OperationResult.SUCCESS -> onSuccess
        }
    }
}
