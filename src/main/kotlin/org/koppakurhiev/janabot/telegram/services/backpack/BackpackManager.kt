package org.koppakurhiev.janabot.telegram.services.backpack

import org.koppakurhiev.janabot.common.getLogger
import org.koppakurhiev.janabot.telegram.bot.ChatData
import org.koppakurhiev.janabot.telegram.bot.ITelegramBot
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOne

class BackpackManager(val bot: ITelegramBot) {
    private val logger = getLogger()
    private val collection = bot.database.getCollection<ChatData>()

    fun createBackpack(chatId: Long, backpackName: String): Boolean {
        val chatData = collection.findOne(ChatData::chatId eq chatId) ?: throw NullPointerException()
        val result = chatData.backpacks.add(Backpack(backpackName))
        collection.updateOne(chatData)
        logger.info { "Backpack $backpackName create in channel $chatId" }
        return result
    }

    fun deleteBackpack(chatId: Long, backpackName: String): Boolean {
        val chatData = collection.findOne(ChatData::chatId eq chatId) ?: throw NullPointerException()
        val result = chatData.backpacks.removeIf { it.name == backpackName }
        collection.updateOne(chatData)
        logger.info { "Backpack $backpackName deleted in channel $chatId" }
        return result
    }

    fun getAll(chatId: Long): List<String> {
        val chatData = collection.findOne(ChatData::chatId eq chatId) ?: throw NullPointerException()
        return chatData.backpacks.map { it.name }
    }

    fun getBackpack(chatId: Long, backpackName: String): Backpack? {
        val chatData = collection.findOne(ChatData::chatId eq chatId) ?: throw NullPointerException()
        return chatData.backpacks.find { it.name == backpackName }
    }

    fun isBackpack(chatId: Long, backpackName: String): Boolean {
        return getBackpack(chatId, backpackName) != null
    }

    fun addToBackpack(chatId: Long, backpackName: String, value: String): Boolean {
        val chatData = collection.findOne(ChatData::chatId eq chatId) ?: throw NullPointerException()
        val backpack = chatData.backpacks.find { it.name == backpackName } ?: throw NullPointerException()
        val result = backpack.content.add(value)
        collection.updateOne(chatData)
        logger.info { "Item added to a backpack at $chatId" }
        return result
    }

    fun removeFromBackpack(chatId: Long, backpackName: String, value: String): Boolean {
        val chatData = collection.findOne(ChatData::chatId eq chatId) ?: throw NullPointerException()
        val backpack = chatData.backpacks.find { it.name == backpackName } ?: throw NullPointerException()
        val result = backpack.content.remove(value)
        collection.updateOne(chatData)
        logger.info { "Item removed form a backpack at $chatId" }
        return result
    }

    fun popRandomFromBackpack(chatId: Long, backpackName: String): String? {
        val chatData = collection.findOne(ChatData::chatId eq chatId) ?: throw NullPointerException()
        val backpack = chatData.backpacks.find { it.name == backpackName } ?: throw NullPointerException()
        val item = backpack.content.randomOrNull()
        if (item != null) {
            backpack.content.remove(item)
            collection.updateOne(chatData)
        }
        logger.info { "Item popped from a backpack at $chatId" }
        return item
    }
}