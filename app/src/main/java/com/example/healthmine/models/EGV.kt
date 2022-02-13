package com.example.healthmine.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

class EGV {
    var systemTime: LocalDateTime? = null
    var displayTime: LocalDateTime? = null
    var value: Int? = null
    var realtimeValue: Int? = null
    var smoothedValue: Int? = null
    var status: String? = null
    var trend: String? = null
    var trendRate: Double? = null
}