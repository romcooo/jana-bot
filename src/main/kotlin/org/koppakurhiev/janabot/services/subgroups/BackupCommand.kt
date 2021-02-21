package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.Conversation
import org.koppakurhiev.janabot.features.MessageLifetime
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.utils.getArg

class BackupCommand(private val subGroupsManager: SubGroupsManager) : ABotService.ACommand("/backup") {

    override suspend fun onCommand(message: Message, s: String?) {
        val conversation = Conversation(message)
        val args = message.text?.split(" ")?.drop(1)
        if (args == null || args.isEmpty()) {
            conversation.replyMessage(JanaBot.messages.get("backup.noCommand"))
            conversation.burnConversation(MessageLifetime.FLASH)
            return
        }
        when (args[0]) {
            "-save" -> backupSubGroups(conversation)
            "-list" -> getAvailableBackups(conversation)
            "-load" -> loadSubGroups(args, conversation)
            "-help" -> {
                conversation.replyMessage(help())
                conversation.burnConversation(MessageLifetime.SHORT)
            }
            else -> {
                conversation.replyMessage(JanaBot.messages.get("backup.unknownCommand", args[0]))
                conversation.burnConversation(MessageLifetime.FLASH)
            }
        }
    }

    override fun help(): String {
        return JanaBot.messages.get("backup.help")
    }

    private suspend fun loadSubGroups(args: List<String>, conversation: Conversation) {
        val index = args.getArg(1)?.toIntOrNull()
        if (index != null && index > subGroupsManager.getAvailableBackups().size) {
            conversation.replyMessage(JanaBot.messages.get("backup.unknownIndex", index))
        }
        val operationResult = if (index == null || index < 1) {
            subGroupsManager.load()
        } else {
            subGroupsManager.load(index)
        }
        val text = when (operationResult) {
            SubGroupsManager.OperationResult.LOAD_FAILED -> {
                logger.error { "Backup load failed!" }
                JanaBot.messages.get("db.loadFailed")
            }
            else -> {
                JanaBot.messages.get("db.loadSucceeded")
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    private suspend fun backupSubGroups(conversation: Conversation) {
        val text = when (subGroupsManager.save(true)) {
            SubGroupsManager.OperationResult.SAVE_FAILED -> {
                logger.error { "Backup save failed!" }
                JanaBot.messages.get("db.saveFailed")
            }
            else -> {
                JanaBot.messages.get("db.saveSucceeded")
            }
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.SHORT)
    }

    private suspend fun getAvailableBackups(conversation: Conversation) {
        val backups = subGroupsManager.getAvailableBackups().mapIndexed { index, backup -> "${index + 1} -> $backup" }
        val text = if (backups.isEmpty()) {
            JanaBot.messages.get("backup.none")
        } else {
            JanaBot.messages.get("backup.active", backups.joinToString(separator = "\n"))
        }
        conversation.replyMessage(text)
        conversation.burnConversation(MessageLifetime.MEDIUM)
    }
}