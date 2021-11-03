package org.koppakurhiev.janabot

import com.mongodb.client.MongoDatabase
import java.util.*

interface IBot {
    val name: String
    val database: MongoDatabase
    val properties: Properties
    val resourceFolder: String

    suspend fun launch()

    suspend fun stop()
}