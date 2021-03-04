package org.koppakurhiev.janabot.persistence

import com.beust.klaxon.Klaxon
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.utils.ALogged
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

abstract class AFileRepository<T>(private val directoryName: String, private val fileName: String) :
    ALogged(), IRepository<T> {
    private val dataFolderPath = JanaBot.properties.getProperty("dataFolder")
    private val maxBackups = JanaBot.properties.getProperty("maxBackups").toInt()
    private val dateFormat = "yyyyMMdd-hhmmss"

    abstract fun load(filePath: String): T?

    private fun getBackupsPath(): String {
        return "$dataFolderPath/$directoryName/backus/"
    }

    private fun getDefaultSavePath(): String {
        return "$dataFolderPath/$directoryName/$fileName.json"
    }

    override fun save(data: T, backup: Boolean): Boolean {
        logger.info { "Saving to $directoryName, backup = $backup" }
        return if (backup) {
            if (storeData(data, "${getBackupsPath()}${getNewBackupFileName()}")) {
                cleanBackups()
                true
            } else {
                false
            }
        } else {
            storeData(data, getDefaultSavePath())
        }
    }

    override fun load(sourceIndex: Int?): T? {
        val filePath = getPathFromIndex(sourceIndex)
        return try {
            load(filePath)
        } catch (e: IOException) {
            logger.warn { "Couldn't load data from $filePath" }
            return null
        }
    }

    override fun getAvailableBackups(): List<String> {
        logger.trace { "Getting available backups." }
        val backupList = File(getBackupsPath()).listFiles()
        backupList?.sortBy { it.lastModified() }
        val stringList = backupList?.map { it.path }
        if (stringList == null) logger.warn { "Backup directory could not be opened" }
        logger.trace { "Available backups: $backupList" }
        return stringList ?: emptyList()
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

    private fun cleanBackups() {
        logger.trace { "Cleaning up backup files" }
        val backups = getAvailableBackups()
        if (backups.size > maxBackups) {
            val toDelete = (backups.size - maxBackups)
            backups.take(toDelete).forEach { fileName ->
                logger.info("Deleting backup $fileName")
                File(fileName).delete()
            }
        }
    }

    private fun getNewBackupFileName(): String {
        val sdf = SimpleDateFormat(dateFormat)
        return "${sdf.format(Date())}_$fileName.json"
    }

    private fun getPathFromIndex(index: Int?): String {
        if (index == null || index <= 0) {
            return getDefaultSavePath()
        }
        val backups = getAvailableBackups()
        if (backups.size < index) {
            throw IndexOutOfBoundsException("Backup index out of bounds: $index")
        }
        return backups[index - 1]
    }
}
