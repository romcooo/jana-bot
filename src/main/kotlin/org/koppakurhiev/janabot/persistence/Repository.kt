package org.koppakurhiev.janabot.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koppakurhiev.janabot.services.ALogged
import org.koppakurhiev.janabot.services.subgroups.SubGroup
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


interface Repository<T> {
    suspend fun save(t: List<T>): Boolean
    suspend fun load(): List<T>
    suspend fun backup(t: List<SubGroup>? = null): Boolean
    suspend fun getAvailableBackups(): List<String>
    suspend fun loadBackup(fileName: String): List<T>
}

private const val FILE_PATH = "src/main/resources/groups.json"
private const val BACKUP_PATH = "src/main/resources/backup/"
private const val BACKUP_FILE_NAME = "groups_backup.json"
private const val DATE_FORMAT = "yyyyMMdd-hhmmss"
//private const val MAX_BACKUPS = 5


class SubGroupSimpleJsonRepository : ALogged(), Repository<SubGroup> {

    override suspend fun save(t: List<SubGroup>): Boolean {
        logger.debug { "Saving: $t" }
        return storeData(t, FILE_PATH)
    }

    override suspend fun load(): List<SubGroup> {
        // TODO try block?
        return try {
            val groups = jacksonObjectMapper().readValue<List<SubGroup>>(File(FILE_PATH))
            logger.debug { "Loaded: $groups" }
            groups
        } catch (e: IOException) {
            logger.trace { "Can't load, returning empty list: ${e.message}"}
            emptyList()
        }
    }

    override suspend fun backup(t: List<SubGroup>?): Boolean {
        val sdf = SimpleDateFormat(DATE_FORMAT)
        return storeData(t ?: load(), BACKUP_PATH + sdf.format(Date()) + BACKUP_FILE_NAME)
    }

    override suspend fun getAvailableBackups(): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun loadBackup(fileName: String): List<SubGroup> {
        TODO("Not yet implemented")
    }

    private suspend fun storeData(t: List<SubGroup>, fileName: String): Boolean {
        val mapper = jacksonObjectMapper()
        withContext(Dispatchers.IO) {
            val data = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(t)
            logger.debug { "Writing data: $data" }
            try {
                val file = File(fileName)
                if (!file.parentFile.exists()) {
                    file.parentFile.mkdirs()
                    file.createNewFile()
                }
                file.writeText(data)
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext false
            }
        }
        return true
    }

    suspend fun cleanBackups() {
        TODO()
    }
}