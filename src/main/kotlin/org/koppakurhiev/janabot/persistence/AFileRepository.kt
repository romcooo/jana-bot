package org.koppakurhiev.janabot.persistence

import com.beust.klaxon.Klaxon
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.utils.ALogged
import java.io.File
import java.io.IOException

abstract class AFileRepository<T>(private val directoryName: String, private val fileName: String) :
    ALogged(), IRepository<T> {
    private val dataFolderPath = JanaBot.properties.getProperty("dataFolder")

    abstract fun load(filePath: String): T?

    private fun getDefaultSavePath(): String {
        return "$dataFolderPath/$directoryName/$fileName.json"
    }

    override fun save(data: T): Boolean {
        logger.info { "Saving to $directoryName" }
        return storeData(data, getDefaultSavePath())
    }

    override fun load(): T? {
        val filePath = getDefaultSavePath()
        return try {
            load(filePath)
        } catch (e: IOException) {
            logger.warn { "Couldn't load data from $filePath" }
            return null
        }
    }

    @Synchronized
    private fun storeData(data: T, filePath: String): Boolean {
        val jsonData = Klaxon().toJsonString(data)
        logger.trace { "Writing to \"$filePath\" data: $jsonData" }
        return try {
            val file = File(filePath)
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            file.writeText(jsonData)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}
