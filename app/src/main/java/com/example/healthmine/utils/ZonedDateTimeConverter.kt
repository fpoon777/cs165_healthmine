package com.example.healthmine.utils

import androidx.room.TypeConverter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// convert local date time for database
class ZonedDateTimeConverter {
    @TypeConverter
    fun toDate(dateString: String?): ZonedDateTime? {
        return try {
            ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toDateString(date: ZonedDateTime?): String? {
        return date?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}