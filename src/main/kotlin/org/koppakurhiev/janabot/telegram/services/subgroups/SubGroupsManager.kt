package org.koppakurhiev.janabot.telegram.services.subgroups

import org.koppakurhiev.janabot.common.ALogged
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.litote.kmongo.*

class SubGroupsManager(val bot: ITelegramBot) : ALogged() {

    private val collection = bot.getDatabase().getCollection<SubGroup>()

    fun createSubGroup(groupName: String, chatId: Long, fromId: Int?): OperationResult {
        if (getSubGroup(chatId, groupName) != null) return OperationResult.GROUP_ALREADY_EXISTS
        logger.debug { "Creating group $groupName for channel $chatId" }
        val newSubgroup = SubGroup(
            name = groupName,
            chatId = chatId,
            admins = if (fromId != null) mutableListOf(fromId) else mutableListOf()
        )
        val insertResult = collection.insertOne(newSubgroup)
        if (!insertResult.wasAcknowledged()) return OperationResult.SAVE_FAILED
        return OperationResult.SUCCESS
    }

    fun addMember(groupName: String, chatId: Long, username: String): OperationResult {
        val currentGroup = getSubGroup(chatId, groupName) ?: return OperationResult.GROUP_NOT_FOUND
        if (currentGroup.members.contains(username)) return OperationResult.GROUP_CONTAINS_MEMBER
        logger.debug { "User $username added to the group $groupName" }
        if (!currentGroup.members.add(username)) return OperationResult.UNKNOWN_ERROR
        return updateGroup(currentGroup)
    }

    private fun updateGroup(currentGroup: SubGroup): OperationResult {
        val updateResult = collection.replaceOneById(currentGroup._id, currentGroup)
        if (!updateResult.wasAcknowledged()) return OperationResult.SAVE_FAILED
        return OperationResult.SUCCESS
    }

    fun removeMember(groupName: String, chatId: Long, username: String): OperationResult {
        val currentGroup = getSubGroup(chatId, groupName) ?: return OperationResult.GROUP_NOT_FOUND
        logger.debug { "User $username removed from the group $groupName" }
        if (!currentGroup.members.remove(username)) return OperationResult.GROUP_MISSING_MEMBER
        return updateGroup(currentGroup)
    }

    fun getSubGroup(chatId: Long, groupName: String): SubGroup? {
        logger.trace { "obtain SubGroup for: chatId=$chatId, group=$groupName" }
        return collection.findOne(SubGroup::chatId eq chatId, SubGroup::name eq groupName)
    }

    fun renameSubGroup(oldGroupName: String, newGroupName: String, chatId: Long): OperationResult {
        val currentGroup = getSubGroup(chatId, oldGroupName) ?: return OperationResult.GROUP_NOT_FOUND
        if (getSubGroup(chatId, newGroupName) != null) {
            return OperationResult.GROUP_ALREADY_EXISTS
        }
        logger.debug { "Renaming group $oldGroupName to $newGroupName" }
        currentGroup.name = newGroupName
        return updateGroup(currentGroup)
    }

    fun deleteSubGroup(groupName: String, chatId: Long): OperationResult {
        logger.debug { "Group $groupName deleted" }
        val result = collection.findOneAndDelete(and(SubGroup::chatId eq chatId, SubGroup::name eq groupName))
        return if (result == null) OperationResult.GROUP_NOT_FOUND else OperationResult.SUCCESS
    }

    fun getMembersList(groupName: String, chatId: Long): List<String>? {
        val currentGroup = getSubGroup(chatId, groupName)
        logger.trace { "Obtained members for $groupName, $chatId" }
        return currentGroup?.members
    }

    fun getChatSubGroups(chatId: Long): List<SubGroup> {
        logger.trace { "Getting SubGroups for chat $chatId" }
        return collection.find(SubGroup::chatId eq chatId).toList()
    }

    fun getAllGroups(): List<SubGroup> {
        logger.trace { "Getting all groups" }
        return collection.find().toList()
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
