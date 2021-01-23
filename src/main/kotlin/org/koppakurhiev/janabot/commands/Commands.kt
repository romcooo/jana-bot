package org.koppakurhiev.janabot.commands

import com.elbekD.bot.Bot
import com.elbekD.bot.types.Message
import org.koppakurhiev.janabot.BotContext

// extension function for the Bot implementation
fun Bot.onCommand(command: Command) {
    // if trigger starts with "/", it's a regular command
    if (command.trigger.startsWith("/")) {
        onCommand(command.trigger, command.action)
    } else { // otherwise, it's a custom command and depends on the message text starting with whatever the trigger is.
        println("in that else")
        onMessage { msg ->
            if (msg.text?.startsWith(command.trigger) == true) {
                println("met the trigger prefix condition")
                command.action.invoke(msg, null)
            }
        }
    }
}

class Command(val trigger: String,
              val action: suspend (Message, String?) -> Unit)

class CustomCommandsForBot(private val bot: Bot) {
    private val currentCommands: List<Command> = listOf(
        helpCommand(),
        startCommand(),
        groupCommand(),
        customGroupTagCommand()
    )

    private fun helpCommand(): Command {
        return Command("/help", ::helpAction)
    }

    private suspend fun helpAction(msg: Message, s: String?): Unit {
        val message = "Hi there, ${msg.from?.first_name ?: "person (it looks like you don't have a first_name)"}!" +
                "\n Currently available commands are:\n" +
                "/start\n" +
                "/help\n" +
                "/g (-create <group-name>, -join <group-name>, -leave <group-name>, -delete <group-name>, -members <group-name>, -list)\n" +
                "!<group-name> <message> to tag people from given group"
        bot.sendMessage(msg.chat.id, message)
    }

    private fun startCommand(): Command {
        return Command("/start", ::startAction)
    }

    private suspend fun startAction(msg: Message, s: String?): Unit {
        bot.sendMessage(msg.chat.id, "Hi, ${msg.from?.first_name ?: "person (it looks like you don't have a first_name)"}!")
    }

    private fun groupCommand(): Command {
        return Command("/g", ::groupAction)
    }

    private suspend fun groupAction(msg: Message, s: String?): Unit {

        val words = msg.text?.split(" ")
        val targetGroupName: String
        if (words != null && words.isNotEmpty() && words.size >= 2) {

            targetGroupName = if (words.size >= 3) {
                words[2]
            } else {
                "" // TODO this is not proper
            }

            val groupsManager = BotContext.groupsManager

            println("groupsManager: $groupsManager")

            when(words[1]) {
                // create a group unless one exists
                "-create" -> {
                    val message = groupsManager.create(targetGroupName, msg.chat.id, msg.from?.id, msg.from?.username)
                    bot.sendMessage(msg.chat.id, message)
                }
                // join the group
                "-join" -> {
                    val message = groupsManager.addMember(targetGroupName, msg.chat.id, msg.from?.username, msg.from?.first_name)
                    bot.sendMessage(msg.chat.id, message)
                }
                // leave the group
                "-leave" -> {
                    val message = groupsManager.removeMember(targetGroupName, msg.chat.id, msg.from?.username)
                    bot.sendMessage(msg.chat.id, message)
                }
                // delete the group (only the original creator can do this)
                "-delete" -> {
                    val message = groupsManager.delete(targetGroupName, msg.chat.id, msg.from?.id)
                    bot.sendMessage(msg.chat.id, message)
                }
                // list all members of group
                "-members" -> {
                    val message = groupsManager.listMembers(targetGroupName, msg.chat.id)
                    bot.sendMessage(msg.chat.id, message)
                }
                // lists all groups in this chat
                "-list" -> {
                    val message = groupsManager.listGroupsInChat(msg.chat.id)
                    bot.sendMessage(msg.chat.id, message)
                }
                // lists all groups (without members)
                "-listall" -> {
                    val message = groupsManager.listAllGroups()
                    bot.sendMessage(msg.chat.id, message)
                }
                // tag everyone in group
                else -> {
                    val message = groupsManager.tagMembers(words[1], msg.chat.id, msg.from?.username)
                    bot.sendMessage(msg.chat.id, message)
                }
            }

        }
    }

    private fun customGroupTagCommand(): Command {
        return Command("!", ::customGroupAction)
    }

    private suspend fun customGroupAction(msg: Message, s: String?): Unit {
        println("in customGroupAction")
        val groupsManager = BotContext.groupsManager
        val groupName = msg.text?.split(" ")?.get(0)?.removePrefix("!")
        if (groupName != null) {
            val message = groupsManager.tagMembers(groupName, msg.chat.id, msg.from?.username)
            bot.sendMessage(msg.chat.id, message)
        } else {
            // TODO take care of this
            println("Oops, it's not my responsibility to send a message here but one should be sent")
        }
    }


    fun addCommands() {
        currentCommands.forEach(bot::onCommand)
    }
}
