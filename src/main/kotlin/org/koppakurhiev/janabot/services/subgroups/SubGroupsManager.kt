package org.koppakurhiev.janabot.services.subgroups

import mu.KotlinLogging

class SubGroupsManager {
    private val logger = KotlinLogging.logger {}
    private val groups: MutableList<SubGroup> = mutableListOf()

    fun load() {
        TODO("Not yet implemented") // in future, load from persistence
    }

    fun createSubGroup(groupName: String, chatId: Long, fromId: Int?): Boolean {
        if (getSubGroup(chatId, groupName) != null) return false
        logger.info { "Creating group $groupName for channel $chatId" }
        groups.add(SubGroup(groupName, chatId, fromId ?: -1))
        return true
    }

    fun addMember(groupName: String, chatId: Long, username: String): Boolean {
        val currentGroup = getSubGroup(chatId, groupName) ?: return false
        if (currentGroup.members.contains(username)) return false
        logger.debug { "User $username added to the group $groupName" }
        return currentGroup.members.add(username)
    }

    fun removeMember(groupName: String, chatId: Long, username: String): Boolean {
        val currentGroup = getSubGroup(chatId, groupName) ?: return false
        logger.debug { "User $username removed from the group $groupName" }
        return currentGroup.members.remove(username)
    }

    fun getSubGroup(chatId: Long, groupName: String): SubGroup? {
        logger.trace { "obtain SubGroup for: chatId=$chatId, group=$groupName" }
        return groups.firstOrNull { g -> g.chatId == chatId && g.name == groupName }
    }

    fun deleteSubGroup(groupName: String, chatId: Long): Boolean {
        val currentGroup = getSubGroup(chatId, groupName) ?: return false
        logger.debug { "Group $groupName deleted" }
        return groups.remove(currentGroup)
    }

    fun getMembersList(groupName: String, chatId: Long): List<String>? {
        val currentGroup = getSubGroup(chatId, groupName)
        logger.trace { "Obtained members for $groupName, $chatId" }
        return currentGroup?.members
    }

    fun getChatSubGroups(chatId: Long): List<SubGroup> {
        logger.trace { "Getting SubGroups for chat $chatId" }
        return groups.filter { g -> g.chatId == chatId }.toList()
    }

    fun getAllGroups(): List<SubGroup> {
        return groups
    }
}