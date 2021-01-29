package org.koppakurhiev.janabot.services.subgroups

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koppakurhiev.janabot.persistence.Repository
import org.koppakurhiev.janabot.persistence.SubGroupSimpleJsonRepository

class SubGroupsManager {
    private val logger = KotlinLogging.logger {}
    private var groups: MutableList<SubGroup> = mutableListOf()
    private val repository: Repository<SubGroup> = SubGroupSimpleJsonRepository()

    // TODO I don't think these should be commands but the current implementation isn't favorable for
    // loading at startup

    // TODO probably move GlobalScope to Repository implementation
    fun load() {
        logger.debug { "Loading groups" }
        GlobalScope.launch {
            groups = try {
                repository.load() as MutableList<SubGroup>
            } catch (e: ClassCastException) {
                logger.debug { "Empty list loaded" }
               mutableListOf()
            }
        }
    }

    fun save() {
        logger.debug { "Backing up and saving groups" }
        GlobalScope.launch {
            repository.backup()
            repository.save(groups)
        }
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