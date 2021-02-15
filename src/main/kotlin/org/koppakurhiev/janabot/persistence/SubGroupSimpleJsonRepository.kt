package org.koppakurhiev.janabot.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koppakurhiev.janabot.JanaBot
import org.koppakurhiev.janabot.persistence.Repository.OperationResultListener
import org.koppakurhiev.janabot.services.subgroups.SubGroup
import org.koppakurhiev.janabot.utils.ALogged
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class SubGroupSimpleJsonRepository : ALogged(), Repository<SubGroup> {
    private val _filePath = JanaBot.properties.getProperty("groups.dataFolder") + "/groups.json"
    private val _backupPath = JanaBot.properties.getProperty("groups.backupFolder")
    private val _backupFileName = "groups_backup.json"
    private val _dateFormat = "yyyyMMdd-hhmmss"
    private val _maxBackups = 3

    override suspend fun save(t: List<SubGroup>, listener: OperationResultListener?) {
        logger.info { "Saving: $t" }
        val result = storeData(t, _filePath)
        listener?.onOperationDone(result)
    }

    // TODO add possible "from"
    override suspend fun load(from: String?): List<SubGroup> {
        return try {
            val groups = jacksonObjectMapper().readValue<List<SubGroup>>(File(_filePath))
            logger.info { "Loaded: $groups" }
            groups
        } catch (e: IOException) {
            logger.warn { "Can't load, returning empty list: ${e.message}" }
            emptyList()
        }
    }

    override suspend fun backup(t: List<SubGroup>?, listener: OperationResultListener?) {
        val sdf = SimpleDateFormat(_dateFormat)
        val result = storeData(t ?: load(), _backupPath + sdf.format(Date()) + _backupFileName)
        listener?.onOperationDone(result)
        cleanBackups()
    }

    override suspend fun getAvailableBackups(listener: OperationResultListener?): List<String> {
        logger.debug { "Getting available backups." }
        val sb = StringBuilder("Available backups: ")
        val list = mutableListOf<String>()
        withContext(Dispatchers.IO) {
            try {
                Files.walk(Paths.get(_backupPath))
                    .filter { item -> item.toString().endsWith("groups_backup.json") }
                    .forEach { item ->
                        sb.append(item.fileName.toString()).append(", ")
                        list.add(item.fileName.toString())
                    }
            } catch (e: IOException) {
                listener?.onOperationDone(false)
            }
        }
        logger.debug { "Available backups retrieved: $sb" }
        listener?.onOperationDone(true, list)
        return list
    }

    private suspend fun storeData(t: List<SubGroup>, fileName: String): Boolean {
        val mapper = jacksonObjectMapper()
        return withContext(Dispatchers.IO) {
            val data = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(t)
            logger.info { "Saving database" }
            logger.trace { "Writing data: $data" }
            try {
                val file = File(fileName)
                if (!file.parentFile.exists()) {
                    file.parentFile.mkdirs()
                    file.createNewFile()
                }
                file.writeText(data)
                return@withContext true
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }

    private suspend fun cleanBackups() {
        logger.debug { "Cleaning up backup files" }
        withContext(Dispatchers.IO) {
            var delCount = 0
            logger.debug { "Backup files: ${File(_backupPath).list()?.toMutableList().toString()}" }
            while (File(_backupPath).list()?.size ?: 0 > _maxBackups) {
                File(_backupPath).listFiles()?.toMutableList()?.minOrNull()?.delete()
                delCount++
            }
            logger.info { "Cleaned up $delCount files." }
        }
    }
}
