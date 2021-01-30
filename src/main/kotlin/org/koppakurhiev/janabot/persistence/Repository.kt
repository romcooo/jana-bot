package org.koppakurhiev.janabot.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koppakurhiev.janabot.services.ALogged
import org.koppakurhiev.janabot.services.subgroups.SubGroup
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*


interface Repository<T> {
    suspend fun save(t: List<T>, listener: OperationResultListener? = null)
    suspend fun load(from: String? = null): List<T>
    suspend fun backup(t: List<SubGroup>? = null, listener: OperationResultListener? = null)
    suspend fun getAvailableBackups(listener: OperationResultListener): List<String>
    suspend fun loadBackup(fileName: String): List<T>
}

interface OperationResultListener {
    fun onOperationDone(result: String)
}

private const val FILE_PATH = "src/main/resources/groups.json"
private const val BACKUP_PATH = "src/main/resources/backup/"
private const val BACKUP_FILE_NAME = "groups_backup.json"
private const val DATE_FORMAT = "yyyyMMdd-hhmmss"
private const val MAX_BACKUPS = 3


class SubGroupSimpleJsonRepository : ALogged(), Repository<SubGroup> {

    override suspend fun save(t: List<SubGroup>, listener: OperationResultListener?) {
        logger.info { "Saving: $t" }
        val result = storeData(t, FILE_PATH)
        listener?.onOperationDone("Save succeeded: $result")
    }

    // TODO add possible "from"
    override suspend fun load(from: String?): List<SubGroup> {
        return try {
            val groups = jacksonObjectMapper().readValue<List<SubGroup>>(File(FILE_PATH))
            logger.info { "Loaded: $groups" }
            groups
        } catch (e: IOException) {
            logger.info { "Can't load, returning empty list: ${e.message}"}
            emptyList()
        }
    }

    override suspend fun backup(t: List<SubGroup>?, listener: OperationResultListener?) {
        val sdf = SimpleDateFormat(DATE_FORMAT)
        val result = storeData(t ?: load(), BACKUP_PATH + sdf.format(Date()) + BACKUP_FILE_NAME)
        listener?.onOperationDone("Backup succeeded: $result")
        cleanBackups()
    }

    override suspend fun getAvailableBackups(listener: OperationResultListener): List<String> {
        val sb = StringBuilder("Available backups: ")
        val list = mutableListOf<String>()
        withContext(Dispatchers.IO) {
            Files
                .walk(Paths.get(BACKUP_PATH))
                .filter { item -> item.toString().endsWith("groups_backup.json") }
                .forEach { item ->
                    sb.append(item.fileName.toString()).append(", ")
                    list.add(item.fileName.toString())
                 }
        }
        listener.onOperationDone(sb.toString())
        return list
    }

    override suspend fun loadBackup(fileName: String): List<SubGroup> {
        TODO("Not yet implemented")
    }

    private suspend fun storeData(t: List<SubGroup>, fileName: String): Boolean {
        val mapper = jacksonObjectMapper()
        return withContext(Dispatchers.IO) {
            val data = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(t)
            logger.debug { "Writing data: $data" }
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
            logger.debug { "Backup files: ${File(BACKUP_PATH).list()?.toMutableList().toString()}" }
            while (File(BACKUP_PATH).list()?.size ?: 0 > MAX_BACKUPS) {
                File(BACKUP_PATH).listFiles()?.toMutableList()?.sorted()?.first()?.delete()
                delCount++
            }
            logger.info { "Cleaned up $delCount files." }
        }

    }
}
