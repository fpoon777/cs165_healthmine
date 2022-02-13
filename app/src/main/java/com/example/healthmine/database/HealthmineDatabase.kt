package com.example.healthmine.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.healthmine.models.AverageEgv
import com.example.healthmine.models.SleepClassifyEventEntity
import com.example.healthmine.models.SleepSegmentEventEntity
import com.example.healthmine.ui.activityrecognition.BackgroundDetectedActivitiesService
import com.example.healthmine.utils.LocalDateTimeConverter
import com.example.healthmine.ui.self_report.self_report_db.SelfReportDbDao
import com.example.healthmine.ui.self_report.self_report_db.SelfReportEntry
import com.example.healthmine.utils.ZonedDateTimeConverter

@Database(entities = [AverageEgv::class, SelfReportEntry::class, ActivityRecognitionEntity::class,
    SleepClassifyEventEntity::class, SleepSegmentEventEntity::class, ActivityTimestampEntity::class], version = 1)
@TypeConverters(LocalDateTimeConverter::class, ZonedDateTimeConverter::class, Converters::class)
abstract class HealthmineDatabase : RoomDatabase() {
    abstract val egvDao: EgvDao
    abstract val selfReportDatabaseDao: SelfReportDbDao
    abstract val activityDatabaseDao: ActivityRecognitionDao
    abstract val sleepClassifyEventDao: SleepClassifyEventDao
    abstract val sleepSegmentEventDao: SleepSegmentEventDao
    abstract val activityTimestampDao: ActivityTimestampDao

    companion object{
        //The Volatile keyword guarantees visibility of changes to the INSTANCE variable across threads
        @Volatile  //Let other thread know this instance is created
        private var INSTANCE: HealthmineDatabase? = null //check the existance of the database, if doesn't have to create one?

        fun getInstance(context: Context) : HealthmineDatabase{  //only can be used in one thread
            synchronized(this){
                var instance = INSTANCE
                if(instance == null){  //Create an instance
                    instance = Room.databaseBuilder(context.applicationContext,  //three parameters: context, what instance we want to create, name of he database,
                        HealthmineDatabase::class.java, "Healthmine_database").build()
                    INSTANCE = instance
                }
                return instance
            }//cannot be implement in multiple thread
        }
    }
}