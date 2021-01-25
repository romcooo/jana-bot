package org.koppakurhiev.janabot.services

interface GroupsManager {
    fun load()
    fun create(groupName: String, chatId: Long, fromId: Int?, fromUsername: String?): String
    fun delete(groupName: String, chatId: Long, userId: Int?): String
    fun addMember(groupName: String, chatId: Long, username: String?, firstName: String?): String
    fun removeMember(groupName: String, chatId: Long, username: String?): String
    fun listMembers(groupName: String, chatId: Long): String
    fun listGroupsInChat(chatId: Long): String
    fun listAllGroups(): String
    fun tagMembers(groupName: String, chatId: Long, username: String?): String
}

class DefaultGroupsManager : GroupsManager {
    companion object {
        const val EMPTY_STRING = "<empty>"
    }

    class SubGroup(
        val name: String,
        val chatId: Long,
        // members by username:
        val members: MutableList<String> = mutableListOf(),
        val creatorId: Int,
        val creatorUsername: String
    ) {

        fun toStringWithChatID(): String {
            return "SubGroup(chatId = $chatId, name='$name', members=$members)"
        }

        override fun toString(): String {
            return "SubGroup(name='$name', members=$members)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SubGroup
            if (name != other.name) return false
            if (chatId != other.chatId) return false
            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + chatId.hashCode()
            return result
        }
    }

    private val groups: MutableList<SubGroup> = mutableListOf()

    override fun load() {
        TODO("Not yet implemented") // in future, load from persistence
    }

    override fun create(groupName: String, chatId: Long, fromId: Int?, fromUsername: String?): String {
        return if (groupName.isNotEmpty()) {
            if (groupForChatWithName(chatId, groupName) == null) {
                groups.add(
                    SubGroup(
                        name = groupName,
                        chatId = chatId,
                        creatorId = fromId ?: -1,
                        creatorUsername = fromUsername ?: EMPTY_STRING
                    )
                )
                println("adding group $groupName, groups: $groups")
                "Created group: $groupName"
            } else {
                "Group with name $groupName already exists"
            }
        } else {
            "Sorry, can't create a group with no name"
        }
    }

    override fun addMember(groupName: String, chatId: Long, username: String?, firstName: String?): String {
        val currentGroup = groupForChatWithName(chatId, groupName)
        return if (currentGroup != null) {
            if (username != null) {
                if (!currentGroup.members.contains(username)) {
                    currentGroup.members.add(username)
                    "User $username (first name: $firstName) added to group $groupName"
                } else {
                    "User $username (first name: $firstName) is already in group $groupName"
                }
            } else {
                "Hey, you don't have a username! Please, set one up in your settings and retry joining, otherwise I can't tag you properly :(."
            }
        } else {
            "Group with name $groupName doesn't exist. Create one using /g -create <name> before adding members."
        }
    }

    override fun removeMember(groupName: String, chatId: Long, username: String?): String {
        val currentGroup = groupForChatWithName(chatId, groupName)
        return if (currentGroup != null) {
            if (currentGroup.members.remove(username)) {
                "User with username ${username ?: EMPTY_STRING} removed from group $groupName."
            } else {
                "$username is not a member of group $groupName."
            }
        } else {
            "Group with name $groupName doesn't exist. Create one using /g -create <name> before adding members."
        }
    }

    override fun delete(groupName: String, chatId: Long, userId: Int?): String {
        val currentGroup = groupForChatWithName(chatId, groupName)
        return if (currentGroup != null) {
            if (currentGroup.creatorId == userId || currentGroup.creatorId == -1) {
                groups.remove(currentGroup)
                "Group ${currentGroup.name} deleted."
            } else {
                "Groups can only be deleted by whoever created them. Group ${currentGroup.name} was created by: ${currentGroup.creatorUsername}"
            }
        } else {
            "Group with name $groupName doesn't exist. Create one using /g -create <name> before adding members."
        }
    }

    override fun listMembers(groupName: String, chatId: Long): String {
        val currentGroup = groupForChatWithName(chatId, groupName)
        return if (currentGroup != null) {
            if (currentGroup.members.isNotEmpty()) {
                "Group $groupName members are: ${currentGroup.members}"
            } else {
                "Group with name $groupName has no members. People can join using /g -join <group-name> before adding members."
            }
        } else {
            "Group with name $groupName doesn't exist. Create one using /g -create <name> before adding members."
        }
    }

    override fun listGroupsInChat(chatId: Long): String {
        val chatGroups = groups.filter { g -> g.chatId == chatId }.toList()
        return if (chatGroups.isNotEmpty()) {
            "Current groups in this chat are: $chatGroups"
        } else {
            "No groups currently exist in this chat."
        }
    }

    override fun listAllGroups(): String {
        return if (groups.isNotEmpty()) {
            val allGroups = StringBuilder()
            groups.forEach { allGroups.append(it.toStringWithChatID()) }
            "Current groups across all chats are: $allGroups}"
        } else {
            "No groups currently exist anywhere."
        }
    }

    override fun tagMembers(groupName: String, chatId: Long, username: String?): String {
        val currentGroup = groupForChatWithName(chatId, groupName)
        return if (currentGroup != null && currentGroup.members.isNotEmpty()) {
            val tags = StringBuilder()
            tags.append("Hey, ")
            for (user in currentGroup.members) {
                tags.append("@$user ")
            }
            tags.append("- ${username ?: EMPTY_STRING} has a message for you, check it out!")
            tags.toString()
        } else {
            "Group with name $groupName not found."
        }
    }

    private fun groupForChatWithName(chatId: Long, name: String): SubGroup? {
        println("groupForChatWithName: chatId = $chatId, name = $name, groups = $groups")
        return groups.firstOrNull { g -> g.chatId == chatId && g.name == name }
    }
}