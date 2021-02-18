package org.koppakurhiev.janabot.persistence

import com.beust.klaxon.Klaxon
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.utils.ALogged
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

abstract class ARepository<T>(private val directoryName: String, private val fileName: String) :
    ALogged(), Repository<T> {
    private val dataFolderPath = JanaBot.properties.getProperty("dataFolder")
    private val maxBackups = JanaBot.properties.getProperty("maxBackups").toInt()
    private val dateFormat = "yyyyMMdd-hhmmss"

    private fun getBackupsPath(): String {
        return "$dataFolderPath/$directoryName/backus/"
    }

    private fun getDefaultSavePath(): String {
        return "$dataFolderPath/$directoryName/$fileName.json"
    }

    override fun save(data: List<T>, backup: Boolean): Boolean {
        logger.info { "Saving to $directoryName" }
        return if (backup) {
            storeData(data, "${getBackupsPath()}${getNewBackupFileName()}")
        } else {
            storeData(data, getDefaultSavePath())
        }
    }

    override fun load(sourceIndex: Int?): List<T>? {
        val filePath = getPathFromIndex(sourceIndex)
        return try {
            load(filePath)
        } catch (e: IOException) {
            logger.warn { "Can't load, returning empty list: ${e.message}" }
            emptyList()
        }
    }

    protected inline fun <reified T> parse(path: String): List<T>? {
        return Klaxon().parseArray(File(path))
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
    private fun storeData(data: List<T>, filePath: String): Boolean {
        val jsonData = Klaxon().toJsonString(data)
        logger.trace { "Writing to \"$filePath\" data: $jsonData" }
        return try {
            val file = File(filePath)
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            file.writeText(jsonData)
            cleanBackups()
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
