package com.example.healthmine.models

import com.google.android.gms.location.SleepSegmentEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class SleepSegmentEventDTO(
    val startTime: String = "",

    val endTime: String = "",

    val status: Int = -1

) {
    constructor() : this("", "", -1)

    companion object {
        fun from(sleepSegmentEventEntity: SleepSegmentEventEntity): SleepSegmentEventDTO {
            return SleepSegmentEventDTO(
                startTime = sleepSegmentEventEntity.startTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                endTime = sleepSegmentEventEntity.endTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                status = sleepSegmentEventEntity.status
            )
        }
    }
}