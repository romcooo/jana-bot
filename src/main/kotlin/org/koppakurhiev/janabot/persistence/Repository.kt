package org.koppakurhiev.janabot.persistence

interface Repository<T> {
    fun save(data: T, backup: Boolean = false): Boolean
    fun load(sourceIndex: Int? = null): T?
    fun getAvailableBackups(): List<String>
    fun load(filePath: String): T?
}