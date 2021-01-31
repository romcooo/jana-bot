package org.koppakurhiev.janabot.services.subgroups

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.features.LivingMessage
import org.koppakurhiev.janabot.sendMessageWithLifetime
import org.koppakurhiev.janabot.services.ABotService
import org.koppakurhiev.janabot.services.IBotService

class SubGroupsService : ABotService() {

    private val subGroupsManager = SubGroupsManager()
    private val regex = Regex("@[a-zA-Z0-9_]+")

    override fun help(): String {
        logger.trace { "SubGroup help called" }
        return "/tag <group-name> - tag all people within the sub-group\n" +
                "/group <action> - possible actions:\n" +
                "    -create <group-name> - create new chat sub-group \n" +
                "    -join <group-name> - join the <group-name> sub-group\n" +
                "    -leave <group-name> - leave the <group-name> sub-group\n" +
                "    -delete <group-name> - delete the <group-name> sub-group (if you are the creator)\n" +
                "    -members <group-name> - lists the members of sub-group <group-name>\n" +
                "    -list - list all the sub-groups  within this chat\n" +
                "    -listAll - lists all groups recognised by this bot\n" +
                "    -saveBackup - forces a backup save of current groups\n" +
                "    -listBackups - lists all stored backup file names (with date and time)\n"
    }

    override fun getCommands(): Array<IBotService.ICommand> {
        return arrayOf(
            GroupCommand(subGroupsManager),
            TagCommand(subGroupsManager),
        )
    }

    override suspend fun onMessage(message: Message) {
        if (message.text != null) {
            val matches = regex.findAll(message.text.toString())
            val taggedChannels = mutableListOf<String>()
            matches.forEach { taggedChannels.add(it.value.drop(1)) }
            val text = TagCommand.tagMembers(subGroupsManager, message.from?.username, message.chat.id, *taggedChannels.toTypedArray())
            text?.let { JanaBot.bot.sendMessageWithLifetime(message.chat.id, text, lifetime = LivingMessage.MessageLifetime.FOREVER) }
        }
    }
}
