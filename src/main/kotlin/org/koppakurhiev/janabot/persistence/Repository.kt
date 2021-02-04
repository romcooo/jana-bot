package org.koppakurhiev.janabot.persistence

import org.koppakurhiev.janabot.services.subgroups.SubGroup


interface Repository<T> {
    suspend fun save(t: List<T>, listener: OperationResultListener? = null)
    suspend fun load(from: String? = null): List<T>
    suspend fun backup(t: List<SubGroup>? = null, listener: OperationResultListener? = null)
    suspend fun getAvailableBackups(listener: OperationResultListener): List<String>
    suspend fun loadBackup(fileName: String): List<T>

    interface OperationResultListener {
        fun onOperationDone(operationName: String = "??", isSuccess: Boolean, data: List<String> = emptyList())
    }
}


