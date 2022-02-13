package com.example.healthmine.utils

import androidx.room.TypeConverter
import java.time.LocalDateTime

// convert local date time for database
class LocalDateTimeConverter {
    @TypeConverter
    fun toDate(dateString: String?): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toDateString(date: LocalDateTime?): String? {
        return date?.toString()
    }
}