package org.koppakurhiev.janabot

import com.elbekD.bot.Bot
import org.koppakurhiev.janabot.core.SubGroup

// TODO add logging

// TODO move this and make private
private var BOT_USERNAME = System.getenv("janaBotUsername")
private var BOT_TOKEN = System.getenv("janaBotToken")

private const val EMPTY = "<empty>"

// TODO overhaul with proper patterns in progress, leave the code below functional until done
fun main() {

    val bot = Bot.createPolling(BOT_USERNAME, BOT_TOKEN)

    // start command
    bot.onCommand("/start") { msg, _ ->
        bot.sendMessage(msg.chat.id, "Hi, ${msg.from?.first_name ?: "person (it looks like you don't have a first_name)"}!")
    }

    // help - list of commands
    // TODO use bot.getMyCommands if applicable
    bot.onCommand("/help") { msg, _ ->
        val message = "Hi there, ${msg.from?.first_name ?: "person (it looks like you don't have a first_name)"}!" +
                "\n Currently available commands are:\n" +
                "/start\n" +
                "/help\n" +
                "/g (-create <group-name>, -join <group-name>, -leave <group-name>, -delete <group-name>, -members <group-name>, -list)\n" +
                "!<group-name> <message> to tag people from given group"
        bot.sendMessage(msg.chat.id, message)
    }

    // define groups for /g
    val groups: MutableList<SubGroup> = mutableListOf()
    // group commands
    bot.onCommand("/g") { msg, _ ->
        val words = msg.text?.split(" ")
        val targetGroupName: String
        if (words != null && words.isNotEmpty() && words.size >= 2) {

            targetGroupName = if (words.size >= 3) {
                words[2]
            } else {
                ""
            }

            val chatGroups = groups.filter{ it.chatId == msg.chat.id}.toMutableList()

            when(words[1]) {
                // create a group unless one exists
                "-create" -> {
                    if (chatGroups.find { it.name == targetGroupName } == null) {
                        groups.add(SubGroup(name = targetGroupName, chatId = msg.chat.id, creatorId = msg.from?.id ?: -1, creatorUsername = msg.from?.username ?: EMPTY))
                        bot.sendMessage(msg.chat.id, "Created group: $targetGroupName")
                    } else {
                        bot.sendMessage(msg.chat.id, "Group with name $targetGroupName already exists")
                    }
                }
                // join the group
                "-join" -> {
                    val currentGroup = chatGroups.find { it.name == targetGroupName }
                    if (currentGroup != null) {
                        val username = msg.from?.username
                        if (username != null) {
                            if (!currentGroup.members.contains(username)) {
                                currentGroup.members.add(username)
                                bot.sendMessage(msg.chat.id, "User $username (first name: ${msg.from?.first_name ?: EMPTY}) added to group ${currentGroup.name}")
                            } else {
                                bot.sendMessage(msg.chat.id, "User $username (first name: ${msg.from?.first_name ?: EMPTY}) is already in group ${currentGroup.name}")
                            }
                        } else {
                            bot.sendMessage(msg.chat.id, "Hey, you don't have a username! Please, set one up in your settings and retry joining, otherwise I can't tag you properly :(.")
                        }
                    } else {
                        bot.sendMessage(msg.chat.id, "Group with name $targetGroupName doesn't exist. Create one using /g -create <name> before adding members.")
                    }
                }
                // leave the group
                "-leave" -> {
                    val currentGroup = chatGroups.find { it.name == targetGroupName }
                    if (currentGroup != null) {
                        val username = msg.from?.username
                        if (currentGroup.members.remove(username)) {
                            bot.sendMessage(msg.chat.id, "User with username $username removed from group ${currentGroup.name}.")
                        } else {
                            bot.sendMessage(msg.chat.id, "$username is not a member of group ${currentGroup.name}.")
                        }
                    } else {
                        bot.sendMessage(msg.chat.id, "Group with name $targetGroupName doesn't exist. Create one using /g -create <name> before adding members.")
                    }
                }
                // delete the group (only the original creator can do this)
                "-delete" -> {
                    val currentGroup = chatGroups.find { it.name == targetGroupName }
                    if (currentGroup != null) {
                        if (currentGroup.creatorId == msg.from?.id || currentGroup.creatorId == -1) {
                            groups.remove(currentGroup)
                            bot.sendMessage(msg.chat.id, "Group ${currentGroup.name} deleted.")
                        } else {
                            bot.sendMessage(msg.chat.id, "Groups can only be deleted by whoever created them. Group ${currentGroup.name} was created by: ${currentGroup.creatorUsername}")
                        }
                    } else {
                        bot.sendMessage(msg.chat.id, "Group with name $targetGroupName doesn't exist. Create one using /g -create <name> before adding members.")
                    }
                }
                // list all members of group
                "-members" -> {
                    val currentGroup = chatGroups.find { it.name == targetGroupName }
                    if (currentGroup != null) {
                        if (currentGroup.members.isNotEmpty()) {
                            bot.sendMessage(msg.chat.id, "Group $targetGroupName members are: ${currentGroup.members}")
                        } else {
                            bot.sendMessage(msg.chat.id, "Group with name $targetGroupName has no members. People can join using /g -join <group-name> before adding members.")
                        }
                    } else {
                        bot.sendMessage(msg.chat.id, "Group with name $targetGroupName doesn't exist. Create one using /g -create <name> before adding members.")
                    }
                }
                // lists all groups in this chat
                "-list" -> {
                    if (chatGroups.isNotEmpty()) {
                        bot.sendMessage(msg.chat.id, "Current groups in this chat are: $chatGroups")
                    } else {
                        bot.sendMessage(msg.chat.id, "No groups currently exist in this chat.")
                    }
                }
                // lists all groups (without members)
                "-listall" -> {
                    if (groups.isNotEmpty()) {
                        val allGroups = StringBuilder()
                        groups.forEach{allGroups.append(it.toStringWithChatID())}
                        bot.sendMessage(msg.chat.id, "Current groups across all chats are: ${allGroups}}")
                    } else {
                        bot.sendMessage(msg.chat.id, "No groups currently exist anywhere.")
                    }
                }
                else -> {
                    val currentGroups = chatGroups.find { it.name == words[1] }
                    if (currentGroups != null && currentGroups.members.isNotEmpty()) {
                        val tags = StringBuilder()
                        tags.append("Hey, ")
                        for (user in currentGroups.members) {
                            tags.append("@$user ")
                        }
                        tags.append("- ${msg.from?.first_name ?: "someone"} has a message for you, check it out!")
                        bot.sendMessage(msg.chat.id, tags.toString())
                    } else {
                        bot.sendMessage(msg.chat.id, "Group with name ${words[1]} not found.")
                    }
                }
            }

        }
    }

    // any message with !groupName also triggers tagging
    // keep in mind that bots do not have access to messages in group chats by default and need to be added as admins
    // in order for this to work
    bot.onMessage { msg ->
        if (msg.text?.startsWith("!") == true) {
            val chatGroups = groups.filter{ it.chatId == msg.chat.id}.toMutableList()
            val groupName = msg.text?.split(" ")?.get(0)?.removePrefix("!")
            val currentGroup = chatGroups.find { it.name == groupName }

            if (currentGroup != null && currentGroup.members.isNotEmpty()) {
                val tags = StringBuilder()
                tags.append("Hey, ")
                for (user in currentGroup.members) {
                    tags.append("@$user ")
                }
                tags.append("- ${msg.from?.first_name ?: "someone"} has a message for you, check it out!")
                bot.sendMessage(msg.chat.id, tags.toString())
            }
        }
    }

    bot.start()
}
