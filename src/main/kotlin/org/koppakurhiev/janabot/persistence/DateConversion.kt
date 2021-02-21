package org.koppakurhiev.janabot.persistence

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.KlaxonException
import java.time.Duration
import java.time.LocalDateTime

@Target(AnnotationTarget.FIELD)
annotation class KlaxonDuration

@Target(AnnotationTarget.FIELD)
annotation class KlaxonLocalDateTime

object LocalDateTimeConverter : Converter {
    override fun canConvert(cls: Class<*>) = cls == LocalDateTime::class.java

    override fun fromJson(jv: JsonValue): LocalDateTime =
        if (jv.string != null) {
            LocalDateTime.parse(jv.string)
        } else {
            throw KlaxonException("Couldn't parse date: ${jv.string}")
        }

    override fun toJson(value: Any): String = """ { "date" : $value } """
}

object DurationConverter : Converter {
    /**
     * @return true if this converter can convert this class.
     */
    override fun canConvert(cls: Class<*>) = cls == Duration::class.java

    /**
     * Convert the given Json value into an object.
     */
    override fun fromJson(jv: JsonValue): Duration =
        if (jv.string != null) {
            Duration.parse(jv.string)
        } else {
            throw KlaxonException("Couldn't parse date: ${jv.string}")
        }

    /**
     * @return the JSON representation of the given value.
     */
    override fun toJson(value: Any): String = """ { "duration" : $value } """
}