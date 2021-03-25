package org.koppakurhiev.janabot.persistence

import com.mongodb.client.MongoDatabase
import org.koppakurhiev.janabot.JanaBot
import org.litote.kmongo.KMongo

object MongoRepository {
    lateinit var db: MongoDatabase

    fun initialize() {
        val client = KMongo.createClient(JanaBot.properties.getProperty("db.string"))
        db = client.getDatabase(JanaBot.properties.getProperty("db.name"))
    }
}