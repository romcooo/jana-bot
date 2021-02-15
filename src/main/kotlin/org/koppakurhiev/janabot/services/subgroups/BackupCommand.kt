package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.ABotService

class BackupCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/backup") {
    override suspend fun onCommand(message: Message, s: String?) {
        val conversation = Conversation(message)
        val args = message.text?.split(" ")?.drop(1)
        if (args == null || args.isEmpty()) {
            conversation.sendMessage(JanaBot.messages.get("backup.unknownCommand"))
            conversation.burnConversation(MessageLifetime.SHORT)
            return
        }
        when(args[0]) {
            "-save" -> backupSubGroups()
            "-list" -> getAvailableBackups(conversation)
            "-load" -> loadSubGroups()
            "-help" -> help()
        }
    }

    override fun help(): String {
        return JanaBot.messages.get("backup.help")
    }

    private fun loadSubGroups() {
        subGroupsManager.load()
    }

    private fun backupSubGroups() {
        subGroupsManager.saveBackup()
    }

    private suspend fun getAvailableBackups(conversation: Conversation) {
        val backups = subGroupsManager.getBackups()
        val text = if (backups.isEmpty()) {
            JanaBot.messages.get("backup.none")
        } else {
            JanaBot.messages.get("backup.active", backups.toString())
        }
        conversation.sendMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }
}