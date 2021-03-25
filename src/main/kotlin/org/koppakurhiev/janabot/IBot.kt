package org.koppakurhiev.janabot

import com.mongodb.client.MongoDatabase
import java.util.*

interface IBot {
    suspend fun launch()

    suspend fun stop()

    fun getName(): String

    fun getResourceFolder(): String

    fun getDatabase(): MongoDatabase

    fun getProperties(): Properties
}