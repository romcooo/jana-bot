package org.koppakurhiev.janabot.persistence

import com.mongodb.client.MongoDatabase
import org.litote.kmongo.KMongo
import java.util.*

class MongoRepository(properties: Properties) {
    val database: MongoDatabase

    init {
        val client = KMongo.createClient(properties.getProperty("db.string"))
        database = client.getDatabase(properties.getProperty("db.name"))
    }
}