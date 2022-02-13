package com.example.healthmine.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "activity_recognition_time_duration_data_table")
data class ActivityRecognitionEntity (  //have an id parameters, and many other workout detail info
    var sequenceId: String = "",

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @ColumnInfo(name = "activity_date")
    var Date: String = "",

    @ColumnInfo(name = "still_activity_recognition_time_duration")
    var StillTime: Float = 0F,

    @ColumnInfo(name = "walk_activity_recognition_time_duration")
    var WalkTime: Float = 0F,

    @ColumnInfo(name = "run_activity_recognition_time_duration")
    var RunTime: Float = 0F,

    @ColumnInfo(name = "vehicle_activity_recognition_time_duration")
    var VehicleTime: Float = 0F,

    @ColumnInfo(name = "bicycle_activity_recognition_time_duration")
    var BicycleTime: Float = 0F,

    @ColumnInfo(name = "on_foot_activity_recognition_time_duration")
    var OnFootTime: Float = 0F,

    @ColumnInfo(name = "tilt_activity_recognition_time_duration")
    var TiltTime: Float = 0F,

    @ColumnInfo(name = "unknown_activity_recognition_time_duration")
    var UnknownTime: Float = 0F,

){
    companion object {
        fun from(sequenceId: String,
                 //id: Long,
                 date: String,
                 stillTime: Float,
                 walkTime: Float,
                 runTime: Float,
                 vehicleTime: Float,
                 bicycleTime: Float,
                 onfootTime: Float,
                 tiltTime: Float,
                 unknownTime: Float): ActivityRecognitionEntity {
            return ActivityRecognitionEntity(
                sequenceId = sequenceId,
                //id = id,
                Date = date,
                StillTime = stillTime,
                WalkTime = walkTime,
                RunTime = runTime,
                VehicleTime = vehicleTime,
                BicycleTime = bicycleTime,
                OnFootTime = onfootTime,
                TiltTime = tiltTime,
                UnknownTime = unknownTime
            )
        }
    }
}