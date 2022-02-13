package com.example.healthmine.ui.self_report.self_report_db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "self_report_table")
data class SelfReportEntry(
    @PrimaryKey(autoGenerate = true)
    var id:Long=0L,

    @ColumnInfo(name = "change_diet")
    var changeDiet:Boolean = false,

    @ColumnInfo(name = "rate_health")
    var rateHealth:Int = -1,

    @ColumnInfo(name = "date_time")
    var dateTime:String = "",

    @ColumnInfo(name = "additional_info")
    var additionalInfo:String="",

    @ColumnInfo(name = "change_medication")
    var changeMedication:Boolean = false,

    @ColumnInfo(name = "other_changes")
    var otherChanges:String=""
){}