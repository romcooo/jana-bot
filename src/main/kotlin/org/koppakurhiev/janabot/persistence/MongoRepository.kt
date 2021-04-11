package org.koppakurhiev.janabot.persistence

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.KMongo
import java.util.*

class MongoRepository(properties: Properties) {
    val database: MongoDatabase

    init {
        open(properties.getProperty("db.string"))
        if (client == null) {
            throw NullPointerException("Database client is null")
        } else {
            database = client!!.getDatabase(properties.getProperty("db.name"))
        }
    }

    companion object {
        private var client: MongoClient? = null

        fun open(dbString: String) {
            if (client == null) {
                client = KMongo.createClient(dbString)
            }
        }

        fun close() {
            client?.close()
            client = null
        }
    }
}