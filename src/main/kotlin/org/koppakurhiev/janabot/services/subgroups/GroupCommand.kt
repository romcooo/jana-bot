package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.MessageCleaner
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.utils.SimpleConversationContext

class GroupCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/group") {
    private lateinit var conversationContext: SimpleConversationContext

    override suspend fun onCommand(message: Message, s: String?) {
        conversationContext = SimpleConversationContext(message.chat.id, message.message_id)
        val words = message.text?.split(" ")
        logger.debug { "Executing command: $words" }
        if (words == null || words.size < 2) {
            conversationContext.sendMessage(JanaBot.messages.get("group.noCommand"), lifetime = MessageLifetime.SHORT)
            return
        }
        val targetGroupName: String? = if (words.size >= 3) words[2] else null
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
                // Use for testing purposes
                "-listAll".toLowerCase() -> getAllSubGroups(message)
                "-rename" -> renameSubGroup(targetGroupName, words[3], message) // TODO words[].. rework ??
                "-backup", "-backups" -> {
                    when (words[2].toLowerCase()) {
                        "-save" -> backupSubGroups()
                        "-list" -> getAvailableBackups()
                    }
                }
//                "-admin", "-admins" -> when (words[3].toLowerCase()) {
//                    "-add" -> TODO("Not yet implemented")
//                    "-leave" -> TODO("Not yet implemented")
//                }
                // TODO allow loading a backup directly
                //            "-load" -> loadSubGroups(filename)
                else -> {
                    logger.debug { "Unknown argument used: ${words[1]}" }
                    conversationContext.sendMessage(
                        text = JanaBot.messages.get("group.unknownCommand", words[1]),
                        lifetime = MessageLifetime.SHORT
                    )
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            logger.debug { "Wrong number of arguments for given command $words" }
            conversationContext.sendMessage(
                text = JanaBot.messages.get("group.invalidArgumentCount", words[1]),
                lifetime = MessageLifetime.SHORT
            )
        }
        MessageCleaner.registerMessage(message, MessageLifetime.SHORT)
    }

    private fun createSubGroup(groupName: String?, message: Message) {
        val text = if (groupName.isNullOrEmpty()) {
            JanaBot.messages.get("group.noName")
        } else {
            if (subGroupsManager.createSubGroup(groupName, message.chat.id, message.from?.id)) {
                JanaBot.messages.get("group.created", groupName)
            } else {
                JanaBot.messages.get("group.nameExists", groupName)
            }
        }
        conversationContext.sendMessage(text, lifetime = MessageLifetime.LONG)
    }

    private fun joinSubGroup(groupName: String?, message: Message) {
        val username = message.from?.username
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            username.isNullOrEmpty() -> JanaBot.messages.get("group.dontHaveUsername")
            subGroupsManager.getSubGroup(message.chat.id, groupName) == null ->
                JanaBot.messages.get("group.notFound", groupName)
            subGroupsManager.addMember(groupName, message.chat.id, username) ->
                JanaBot.messages.get("group.userAdded", username, groupName)
            else -> JanaBot.messages.get("group.userAlreadyIn", username, groupName)
        }
        conversationContext.sendMessage(text, lifetime = MessageLifetime.LONG)
    }

    // TODO currently not possible to validate, if the usernameToAdd is actually in the chat. Probably
    //          will need to rework the SubGroup to store user IDs as well in the future for this to be possible.
    private fun addMember(groupName: String?, usernameToAdd: String?, message: Message) {
        val callerUsername = message.from?.username
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            callerUsername.isNullOrEmpty() -> JanaBot.messages.get("group.dontHaveUsername")
            usernameToAdd.isNullOrEmpty() -> JanaBot.messages.get("group.noUsername")
            subGroupsManager.getSubGroup(message.chat.id, groupName) == null ->
                JanaBot.messages.get("group.notFound", groupName)
            subGroupsManager.addMember(groupName, message.chat.id, usernameToAdd) ->
                JanaBot.messages.get("group.userAdded", usernameToAdd, groupName)
            else -> JanaBot.messages.get("group.userAlreadyIn", usernameToAdd, groupName)
        }
        conversationContext.sendMessage(text, lifetime = MessageLifetime.LONG)
    }

    private fun removeMember(groupName: String?, usernameToRemove: String?, message: Message) {
        val callerUsername = message.from?.username
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            callerUsername.isNullOrEmpty() -> JanaBot.messages.get("group.dontHaveUsername")
            usernameToRemove.isNullOrEmpty() -> JanaBot.messages.get("group.noUsername")
            else -> {
                val group = subGroupsManager.getSubGroup(message.chat.id, groupName)
                when {
                    group == null -> JanaBot.messages.get("group.notFound", groupName)
                    !group.admins.contains(message.from?.id) -> {
                        val adminUsernames = group.admins.map {
                            JanaBot.bot.getChatMember(message.chat, it.toLong()).get().user.username
                        }
                        JanaBot.messages.get("group.noPrivileges", groupName, adminUsernames.toString())
                    }
                    subGroupsManager.removeMember(groupName, message.chat.id, usernameToRemove) ->
                        JanaBot.messages.get("group.userRemoved", usernameToRemove, groupName, callerUsername)
                    else -> JanaBot.messages.get("group.userNotIn", usernameToRemove, groupName)
                }
            }
        }
        conversationContext.sendMessage(text, lifetime = MessageLifetime.LONG)
    }

    private fun leaveSubGroup(groupName: String?, message: Message) {
        val username = message.from?.username
        val text = when {
            groupName.isNullOrEmpty() -> JanaBot.messages.get("group.noName")
            username.isNullOrEmpty() -> JanaBot.messages.get("group.dontHaveUsername")
            subGroupsManager.getSubGroup(message.chat.id, groupName) == null ->
                JanaBot.messages.get("group.notFound", groupName)
            subGroupsManager.removeMember(groupName, message.chat.id, username) ->
                JanaBot.messages.get("group.userLeft", username, groupName)
            else -> JanaBot.messages.get("group.userNotIn", username, groupName)
        }
        conversationContext.sendMessage(text, lifetime = MessageLifetime.LONG)
    }

    private fun renameSubGroup(oldGroupName: String?, newGroupName: String?, message: Message) {
        val text =
            if (oldGroupName.isNullOrEmpty() || newGroupName.isNullOrEmpty()) {
                JanaBot.messages.get("group.noName")
            } else {
                val group = subGroupsManager.getSubGroup(message.chat.id, oldGroupName)
                when {
                    (group == null) -> JanaBot.messages.get("group.notFound", oldGroupName)
                    !group.admins.contains(message.from?.id) -> {
                        val adminUsernames = group.admins.map {
                            JanaBot.bot.getChatMember(message.chat, it.toLong()).get().user.username
                        }
                        JanaBot.messages.get("group.noPrivileges", oldGroupName, adminUsernames.toString())
                    }
                    subGroupsManager.getSubGroup(message.chat.id, newGroupName) != null ->
                        JanaBot.messages.get("group.nameExists", newGroupName)
                    subGroupsManager.renameSubGroup(oldGroupName, newGroupName, message.chat.id) ->
                        JanaBot.messages.get("group.renamed", oldGroupName, newGroupName)
                    else -> {
                        logger.warn { "Rename method failed, this should not happen" }
                        JanaBot.messages.get("group.notFound", oldGroupName)
                    }
                }
            }
        conversationContext.sendMessage(text, lifetime = MessageLifetime.LONG)

    }

    private fun deleteSubGroup(groupName: String?, message: Message) {
        val user = message.from
        val text = if (groupName.isNullOrEmpty()) JanaBot.messages.get("group.noName")
        else if (user?.username == null) JanaBot.messages.get("group.dontHaveUsername")
        else {
            val group = subGroupsManager.getSubGroup(message.chat.id, groupName)
            if (group == null) JanaBot.messages.get("group.notFound", groupName)
            else if (group.admins.isEmpty() || !group.admins.contains(user.id)) {
                val adminUsernames = group.admins.map {
                    JanaBot.bot.getChatMember(message.chat, it.toLong()).get().user.username
                }
                JanaBot.messages.get("group.noPrivileges", groupName, adminUsernames.toString())
            } else if (subGroupsManager.deleteSubGroup(groupName, message.chat.id)) {
                JanaBot.messages.get("group.deleted", groupName)
            } else {
                logger.warn { "Group not found within delete method, this should not happen" }
                JanaBot.messages.get("group.notFound", groupName)
            }
        }
        conversationContext.sendMessage(text, lifetime = MessageLifetime.LONG)
    }

    private fun getSubGroupMembers(groupName: String?, message: Message) {
        val text = if (groupName.isNullOrEmpty()) JanaBot.messages.get("group.noName")
        else {
            val members = subGroupsManager.getMembersList(groupName, message.chat.id)
            when {
                members == null -> JanaBot.messages.get("group.notFound", groupName)
                members.isEmpty() -> JanaBot.messages.get("group.noMembers", groupName)
                else -> JanaBot.messages.get("group.printGroup", groupName, members.toString())
            }
        }
        conversationContext.sendMessage(text, lifetime = MessageLifetime.SHORT)
    }

    private fun getChatSubgroups(message: Message) {
        val subGroups = subGroupsManager.getChatSubGroups(message.chat.id)
        val text = if (subGroups.isEmpty()) JanaBot.messages.get("group.noGroups")
        else {
            val subGroupsString = StringBuilder()
            subGroups.forEach {
                if (it.members.isEmpty()) {
                    subGroupsString.appendLine(JanaBot.messages.get("group.noMembers", it.name))
                } else {
                    subGroupsString
                        .appendLine(JanaBot.messages.get("group.printGroup", it.name, it.members.toString()))
                }
            }
            JanaBot.messages.get("group.chatGroups", subGroupsString.toString())
        }
        conversationContext.sendMessage(text, lifetime = MessageLifetime.SHORT)
    }

    private fun getAllSubGroups(message: Message) {
        val groups = subGroupsManager.getAllGroups()
        val text = if (groups.isNotEmpty()) {
            JanaBot.messages.get("group.allGroups", groups.joinToString("\n"))
        } else {
            JanaBot.messages.get("group.noneExist")
        }
        conversationContext.sendMessage(text, lifetime = MessageLifetime.SHORT)
        MessageCleaner.registerMessage(message, MessageLifetime.SHORT)
    }

    // TODO add possibility to load a backup by passing a parameter
    private fun loadSubGroups() {
        subGroupsManager.load()
    }

    private fun backupSubGroups() {
        subGroupsManager.saveBackup()
    }

    private suspend fun getAvailableBackups() {
        val backups = subGroupsManager.getBackups()
        val text = if (backups.isEmpty()) {
            JanaBot.messages.get("backups.none")
        } else {
            JanaBot.messages.get("backups.active", backups.toString())
        }
        conversationContext.sendMessage(text, MessageLifetime.SHORT)
    }
}
