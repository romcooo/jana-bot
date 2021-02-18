package org.koppakurhiev.janabot.persistence


interface Repository<T> {
    fun save(data: List<T>, backup: Boolean = true): Boolean
    fun load(sourceIndex : Int? = null): List<T>?
    fun getAvailableBackups(): List<String>
    fun load(filePath: String): List<T>?
}