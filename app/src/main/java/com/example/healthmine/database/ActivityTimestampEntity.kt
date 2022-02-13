package com.example.healthmine.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.healthmine.models.SleepSegmentEventEntity
import com.google.android.gms.location.SleepSegmentEvent
import com.google.common.reflect.TypeToken
import java.lang.reflect.Type


@Entity(tableName = "activity_timestamp_table")
data class ActivityTimestampEntity(
    //have an id parameters, and many other workout detail info
    var id: String = "",

    @PrimaryKey(autoGenerate = true)
    var new_id: Long = 0L,

//    @ColumnInfo(name = "activity_date")
//    var Date: String = "",

    @ColumnInfo(name = "timestamps")
    var TimestampList: String = "",

    @ColumnInfo(name = "activity_type")
    var ActivityType: String = "",

    ) {
    companion object {
        fun from(id: String, timestampList: String, activityType: String): ActivityTimestampEntity {
            return ActivityTimestampEntity(
                id = id,
                TimestampList = timestampList,
                ActivityType = activityType,
            )
        }
    }
}