package org.koppakurhiev.janabot.services

import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.ABotService
import org.koppakurhiev.janabot.ACommand
import org.koppakurhiev.janabot.ICommand
import org.koppakurhiev.janabot.JanaBot

class SubGroupsService : ABotService() {

    private val groupsManager = DefaultGroupsManager()
    private val regex = Regex("@[a-zA-Z0-9_]+")

    class SubGroupCommand(private val groupsManager: GroupsManager) : ACommand("/group") {
        override fun onCommand(message: Message, s: String?) {
            val bot = JanaBot.bot
            val words = message.text?.split(" ")
            val targetGroupName: String
            if (words != null && words.isNotEmpty() && words.size >= 2) {

                targetGroupName = if (words.size >= 3) {
                    words[2]
                } else {
                    "" // TODO this is not proper
                }

                println("groupsManager: $groupsManager")

                when (words[1]) {
                    // create a group unless one exists
                    "-create" -> {
                        val text =
                            groupsManager.create(
                                targetGroupName,
                                message.chat.id,
                                message.from?.id,
                                message.from?.username
                            )
                        bot.sendMessage(message.chat.id, text)
                    }
                    // join the group
                    "-join" -> {
                        val text =
                            groupsManager.addMember(
                                targetGroupName,
                                message.chat.id,
                                message.from?.username,
                                message.from?.first_name
                            )
                        bot.sendMessage(message.chat.id, text)
                    }
                    // leave the group
                    "-leave" -> {
                        val text = groupsManager.removeMember(targetGroupName, message.chat.id, message.from?.username)
                        bot.sendMessage(message.chat.id, text)
                    }
                    // delete the group (only the original creator can do this)
                    "-delete" -> {
                        val text = groupsManager.delete(targetGroupName, message.chat.id, message.from?.id)
                        bot.sendMessage(message.chat.id, text)
                    }
                    // list all members of group
                    "-members" -> {
                        val text = groupsManager.listMembers(targetGroupName, message.chat.id)
                        bot.sendMessage(message.chat.id, text)
                    }
                    // lists all groups in this chat
                    "-list" -> {
                        val text = groupsManager.listGroupsInChat(message.chat.id)
                        bot.sendMessage(message.chat.id, text)
                    }
                    // lists all groups (without members)
                    "-listAll" -> {
                        val text = groupsManager.listAllGroups()
                        bot.sendMessage(message.chat.id, text)
                    }
                    // tag everyone in group
                    else -> {
                        val text = groupsManager.tagMembers(words[1], message.chat.id, message.from?.username)
                        bot.sendMessage(message.chat.id, text)
                    }
                }

            }
        }
    }

    override fun help(): String {
        logger.trace { "SubGroup help called" }
        return "/group <action> - possible actions:\n" +
                "    -create <group-name> - create new chat sub-group \n" +
                "    -join <group-name> - join the <group-name> sub-group\n" +
                "    -leave <group-name> - leave the <group-name> sub-group\n" +
                "    -delete <group-name> - delete the <group-name> sub-group (if you are the creator)\n" +
                "    -members <group-name> - lists the members of sub-group <group-name>\n" +
                "    -list - list all the sub-groups  within this chat\n" +
                "    -listAll - lists all groups recognised by this bot\n"
    }

    override fun getCommands(): Array<ICommand> {
        return arrayOf(SubGroupCommand(groupsManager))
    }

    override suspend fun onMessage(message: Message) {
        message.text?.let { messageText ->
            regex.findAll(messageText).forEach {
                val text = groupsManager.tagMembers(it.value.drop(1), message.chat.id, message.from?.username)
                JanaBot.bot.sendMessage(message.chat.id, text)
            }
        }
    }
}