package com.example.healthmine.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "average_egvs")
class AverageEgv {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    var systemTime: LocalDateTime? = null
    var value: Int? = null
    var tag: Int? = null
}