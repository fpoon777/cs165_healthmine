package com.example.healthmine.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.location.SleepSegmentEvent
import java.time.*
import java.time.format.DateTimeFormatter

@Entity(tableName = "sleep_segment_events")
data class SleepSegmentEventEntity(
    var id: String?,

    @PrimaryKey
    val startTime: ZonedDateTime = Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneOffset.UTC),

    val endTime: ZonedDateTime = Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneOffset.UTC),

    val status: Int = -1

) {
    constructor() : this(
        null,
        Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneOffset.UTC),
        Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneOffset.UTC),
        -1
    )

    companion object {
        fun from(id: String?, sleepSegmentEvent: SleepSegmentEvent): SleepSegmentEventEntity {
            return SleepSegmentEventEntity(
                id = id,
                startTime = Instant.ofEpochMilli(sleepSegmentEvent.startTimeMillis)
                    .atZone(ZoneId.systemDefault()),
                endTime = Instant.ofEpochMilli(sleepSegmentEvent.endTimeMillis)
                    .atZone(ZoneId.systemDefault()),
                status = sleepSegmentEvent.status
            )
        }

        fun fromDTO(id: String?, sleepSegmentEventDTO: SleepSegmentEventDTO): SleepSegmentEventEntity {
            return SleepSegmentEventEntity(
                id = id,
                startTime = ZonedDateTime.parse(sleepSegmentEventDTO.startTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                endTime = ZonedDateTime.parse(sleepSegmentEventDTO.endTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                status = sleepSegmentEventDTO.status
            )
        }
    }
}