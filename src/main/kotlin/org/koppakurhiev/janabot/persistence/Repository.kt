package org.koppakurhiev.janabot.persistence

import org.koppakurhiev.janabot.services.subgroups.SubGroup


interface Repository<T> {
    suspend fun save(t: List<T>, listener: OperationResultListener? = null)
    suspend fun load(from: String? = null): List<T>
    suspend fun backup(t: List<SubGroup>? = null, listener: OperationResultListener? = null)
    suspend fun getAvailableBackups(listener: OperationResultListener? = null): List<String>

    interface OperationResultListener {
        fun onOperationDone(isSuccess: Boolean, data: List<String> = emptyList())
    }
}