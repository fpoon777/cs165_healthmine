package com.example.healthmine.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.location.SleepClassifyEvent

@Entity(tableName = "sleep_classify_events")
data class SleepClassifyEventEntity(
    @PrimaryKey
    val timestampSeconds: Int,

    val confidence: Int,

    val motion: Int,

    val light: Int
) {
    companion object {
        fun from(sleepClassifyEvent: SleepClassifyEvent): SleepClassifyEventEntity {
            return SleepClassifyEventEntity(
                timestampSeconds = (sleepClassifyEvent.timestampMillis / 1000).toInt(),
                confidence = sleepClassifyEvent.confidence,
                motion = sleepClassifyEvent.motion,
                light = sleepClassifyEvent.light
            )
        }
    }
}