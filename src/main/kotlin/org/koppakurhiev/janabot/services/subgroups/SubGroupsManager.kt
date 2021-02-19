package org.koppakurhiev.janabot.services.subgroups

import org.koppakurhiev.janabot.utils.ALogged

class SubGroupsManager : ALogged() {

    @Volatile
    private lateinit var groups: MutableList<SubGroup>
    private val repository = SubGroupRepository("groups", "groups")

    init {
        load()
    }

    fun load(sourceIndex: Int? = null): OperationResult {
        logger.debug { "Loading groups" }
        val loadResult: List<SubGroup>? = repository.load(sourceIndex)
        groups = loadResult?.toMutableList() ?: mutableListOf()
        return if (loadResult == null) OperationResult.LOAD_FAILED else OperationResult.SUCCESS
    }

    fun save(backup: Boolean = false): OperationResult {
        logger.debug { "Saving groups" }
        return if (repository.save(groups, backup)) OperationResult.SUCCESS else OperationResult.SAVE_FAILED
    }

    fun getAvailableBackups(): List<String> {
        return repository.getAvailableBackups()
    }

    fun createSubGroup(groupName: String, chatId: Long, fromId: Int?): OperationResult {
        if (getSubGroup(chatId, groupName) != null) return OperationResult.GROUP_ALREADY_EXISTS
        logger.debug { "Creating group $groupName for channel $chatId" }
        if (!groups.add(
                SubGroup(
                    name = groupName,
                    chatId = chatId,
                    admins = if (fromId != null) mutableListOf(fromId) else mutableListOf()
                )
            )
        ) return OperationResult.UNKNOWN_ERROR
        return save()
    }

    fun addMember(groupName: String, chatId: Long, username: String): OperationResult {
        val currentGroup = getSubGroup(chatId, groupName) ?: return OperationResult.GROUP_NOT_FOUND
        if (currentGroup.members.contains(username)) return OperationResult.GROUP_CONTAINS_MEMBER
        logger.debug { "User $username added to the group $groupName" }
        if (!currentGroup.members.add(username)) return OperationResult.UNKNOWN_ERROR
        return save()
    }

    fun removeMember(groupName: String, chatId: Long, username: String): OperationResult {
        val currentGroup = getSubGroup(chatId, groupName) ?: return OperationResult.GROUP_NOT_FOUND
        logger.debug { "User $username removed from the group $groupName" }
        if (!currentGroup.members.remove(username)) return OperationResult.GROUP_MISSING_MEMBER
        return save()
    }

    fun getSubGroup(chatId: Long, groupName: String, ignoreCase: Boolean = true): SubGroup? {
        logger.trace { "obtain SubGroup for: chatId=$chatId, group=$groupName" }
        return groups.firstOrNull { g -> g.chatId == chatId && g.name.equals(groupName, ignoreCase = ignoreCase) }
    }

    fun renameSubGroup(oldGroupName: String, newGroupName: String, chatId: Long): OperationResult {
        val currentGroup = getSubGroup(chatId, oldGroupName) ?: return OperationResult.GROUP_NOT_FOUND
        if (groups.find { it.name.equals(oldGroupName, ignoreCase = true) } == null) {
            return OperationResult.GROUP_ALREADY_EXISTS
        }
        logger.debug { "Renaming group $oldGroupName to $newGroupName" }
        currentGroup.name = newGroupName
        return save()
    }

    fun deleteSubGroup(groupName: String, chatId: Long): OperationResult {
        val currentGroup = getSubGroup(chatId, groupName) ?: return OperationResult.GROUP_NOT_FOUND
        logger.debug { "Group $groupName deleted" }
        if (!groups.remove(currentGroup)) return OperationResult.UNKNOWN_ERROR
        return save()
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
        logger.trace { "Getting all groups" }
        return groups
    }

    fun getAdmins(chatId: Long, groupName: String): List<Int>? {
        val currentGroup = getSubGroup(chatId, groupName)
        return currentGroup?.admins
    }

    fun isGroupAdmin(chatId: Long, groupName: String, userId: Int): OperationResult {
        val currentGroup = getSubGroup(chatId, groupName) ?: return OperationResult.GROUP_NOT_FOUND
        if (currentGroup.admins.isEmpty()) return OperationResult.SUCCESS
        if (!currentGroup.admins.contains(userId)) return OperationResult.NOT_GROUP_ADMIN
        return OperationResult.SUCCESS
    }

    enum class OperationResult {
        GROUP_NOT_FOUND,
        GROUP_ALREADY_EXISTS,
        GROUP_MISSING_MEMBER,
        GROUP_CONTAINS_MEMBER,
        NOT_GROUP_ADMIN,
        LOAD_FAILED,
        SAVE_FAILED,
        UNKNOWN_ERROR,
        SUCCESS
    }
}
