package org.koppakurhiev.janabot.persistence

interface IRepository<T> {
    fun save(data: T): Boolean
    fun load(): T?
}