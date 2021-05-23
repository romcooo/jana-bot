package org.koppakurhiev.janabot.telegram.services.subgroups

import com.elbekD.bot.types.User
import org.koppakurhiev.janabot.common.getLogger
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.koppakurhiev.janabot.telegram.bot.getUsername
import org.litote.kmongo.*

class SubGroupsManager(val bot: ITelegramBot) {
    private val logger = getLogger()
    private val collection = bot.database.getCollection<SubGroup>()

    fun createSubGroup(groupName: String, chatId: Long, creatorId: Long): OperationResult {
        if (getSubGroup(chatId, groupName) != null) return OperationResult.GROUP_ALREADY_EXISTS
        logger.debug { "$chatId - Creating group $groupName" }
        val newSubgroup = SubGroup(
            name = groupName,
            chatId = chatId,
            admins = mutableListOf(creatorId)
        )
        val insertResult = collection.insertOne(newSubgroup)
        if (!insertResult.wasAcknowledged()) return OperationResult.SAVE_FAILED
        return OperationResult.SUCCESS
    }

    fun addMember(groupName: String, chatId: Long, user: User): OperationResult {
        val currentGroup = getSubGroup(chatId, groupName) ?: return OperationResult.GROUP_NOT_FOUND
        if (currentGroup.members.contains(user.id.toLong())) return OperationResult.GROUP_CONTAINS_MEMBER
        logger.debug { "$chatId - User ${user.username} added to the group $groupName" }
        if (!currentGroup.members.add(user.id.toLong())) return OperationResult.UNKNOWN_ERROR
        return updateGroup(currentGroup)
    }

    private fun updateGroup(currentGroup: SubGroup): OperationResult {
        val updateResult = collection.replaceOneById(currentGroup._id, currentGroup)
        if (!updateResult.wasAcknowledged()) return OperationResult.SAVE_FAILED
        logger.trace { "${currentGroup.chatId} - Updating group ${currentGroup.name}" }
        return OperationResult.SUCCESS
    }

    fun removeMember(groupName: String, chatId: Long, user: User): OperationResult {
        val currentGroup = getSubGroup(chatId, groupName) ?: return OperationResult.GROUP_NOT_FOUND
        logger.debug { "$chatId - User ${user.username} removed from the group $groupName" }
        if (!currentGroup.members.remove(user.id.toLong())) return OperationResult.GROUP_MISSING_MEMBER
        return updateGroup(currentGroup)
    }

    private fun getSubGroup(chatId: Long, groupName: String): SubGroup? {
        logger.trace { "$chatId - Obtaining SubGroup '$groupName'" }
        return collection.findOne(SubGroup::chatId eq chatId, SubGroup::name eq groupName)
    }

    fun renameSubGroup(oldGroupName: String, newGroupName: String, chatId: Long): OperationResult {
        val currentGroup = getSubGroup(chatId, oldGroupName) ?: return OperationResult.GROUP_NOT_FOUND
        if (getSubGroup(chatId, newGroupName) != null) {
            return OperationResult.GROUP_ALREADY_EXISTS
        }
        logger.debug { "$chatId - Renaming group $oldGroupName to $newGroupName" }
        currentGroup.name = newGroupName
        return updateGroup(currentGroup)
    }

    fun deleteSubGroup(groupName: String, chatId: Long): OperationResult {
        logger.debug { "$chatId - Group $groupName deleted" }
        val result = collection.findOneAndDelete(and(SubGroup::chatId eq chatId, SubGroup::name eq groupName))
        return if (result == null) OperationResult.GROUP_NOT_FOUND else OperationResult.SUCCESS
    }

    suspend fun getMembersList(groupName: String, chatId: Long): List<String>? {
        val currentGroup = getSubGroup(chatId, groupName)
        logger.trace { "$chatId - Obtained members for $groupName" }
        return currentGroup?.members?.mapNotNull { bot.telegramBot.getUsername(chatId, it) }
    }

    fun getChatSubGroups(chatId: Long): List<SubGroup> {
        logger.trace { "$chatId - Getting SubGroups" }
        return collection.find(SubGroup::chatId eq chatId).toList()
    }

    fun getAllGroups(): List<SubGroup> {
        logger.trace { "Getting all groups" }
        return collection.find().toList()
    }

    fun addAdmin(chatId: Long, groupName: String, userId: Long): OperationResult {
        return when (val currentGroup = getSubGroup(chatId, groupName)) {
            null -> OperationResult.GROUP_NOT_FOUND
            else -> {
                logger.debug { "$chatId - Adding group admin for '$groupName'" }
                currentGroup.admins.add(userId)
                updateGroup(currentGroup)
            }
        }
    }

    fun removeAdmin(chatId: Long, groupName: String, userId: Long): OperationResult {
        return when (val currentGroup = getSubGroup(chatId, groupName)) {
            null -> OperationResult.GROUP_NOT_FOUND
            else -> {
                logger.debug { "$chatId - Removing group admin for '$groupName'" }
                currentGroup.admins.remove(userId)
                updateGroup(currentGroup)
            }
        }
    }

    suspend fun getAdmins(chatId: Long, groupName: String): List<String>? {
        val currentGroup = getSubGroup(chatId, groupName)
        return currentGroup?.admins?.mapNotNull { bot.telegramBot.getUsername(chatId, it) }
    }

    fun isGroupAdmin(chatId: Long, groupName: String, userId: Long): OperationResult {
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
