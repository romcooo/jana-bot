package org.koppakurhiev.janabot.services.subgroups

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koppakurhiev.janabot.persistence.Repository
import org.koppakurhiev.janabot.persistence.Repository.OperationResultListener
import org.koppakurhiev.janabot.persistence.SubGroupSimpleJsonRepository

class SubGroupsManager {
    private val logger = KotlinLogging.logger {}
    private lateinit var groups: MutableList<SubGroup>
    private val repository: Repository<SubGroup> = SubGroupSimpleJsonRepository()

    init {
        load()
    }

    fun load() {
        logger.debug { "Loading groups" }
        GlobalScope.launch {
            groups = try {
                repository.load() as MutableList<SubGroup>
            } catch (e: ClassCastException) {
                logger.debug { "Problem loading groups, probably empty datasource: ${e.message}" }
                mutableListOf()
            }
        }
    }

    private fun save() {
        logger.debug { "Saving groups" }
        GlobalScope.launch {
            repository.save(groups)
        }
    }

    private inline fun <reified T> T.andSave(): T {
        save()
        return this
    }

    fun saveBackup() {
        logger.info { "Backing up groups" }
        GlobalScope.launch {
            repository.backup(groups)
        }
    }

    fun getBackups(listener: OperationResultListener) {
        GlobalScope.launch {
            repository.getAvailableBackups(listener)
        }
    }

    fun createSubGroup(groupName: String, chatId: Long, fromId: Int?): Boolean {
        if (getSubGroup(chatId, groupName) != null) return false
        logger.info { "Creating group $groupName for channel $chatId" }
        return groups.add(SubGroup(groupName, chatId, fromId ?: -1)).andSave()
    }

    fun addMember(groupName: String, chatId: Long, username: String): Boolean {
        val currentGroup = getSubGroup(chatId, groupName) ?: return false
        if (currentGroup.members.contains(username)) return false
        logger.debug { "User $username added to the group $groupName" }
        return currentGroup.members.add(username).andSave()
    }

    fun removeMember(groupName: String, chatId: Long, username: String): Boolean {
        val currentGroup = getSubGroup(chatId, groupName) ?: return false
        logger.debug { "User $username removed from the group $groupName" }
        return currentGroup.members.remove(username).andSave()
    }

    fun getSubGroup(chatId: Long, groupName: String): SubGroup? {
        logger.trace { "obtain SubGroup for: chatId=$chatId, group=$groupName" }
        return groups.firstOrNull { g -> g.chatId == chatId && g.name == groupName }
    }

    fun deleteSubGroup(groupName: String, chatId: Long): Boolean {
        val currentGroup = getSubGroup(chatId, groupName) ?: return false
        logger.debug { "Group $groupName deleted" }
        return groups.remove(currentGroup).andSave()
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
