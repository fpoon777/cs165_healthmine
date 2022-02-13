package com.example.healthmine.database

import androidx.room.*
import com.example.healthmine.models.SleepSegmentEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Dao
interface SleepSegmentEventDao {
    @Query("SELECT * FROM sleep_segment_events ORDER BY startTime DESC")
    fun getAll(): Flow<List<SleepSegmentEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sleepSegmentEventEntity: SleepSegmentEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sleepSegmentEventEntities: List<SleepSegmentEventEntity>)

    @Query("SELECT * FROM sleep_segment_events WHERE status = 0 AND endTime BETWEEN :startTime AND :endTime")
    suspend fun getSleepSegmentEndBetween(startTime: ZonedDateTime, endTime: ZonedDateTime): List<SleepSegmentEventEntity>

    @Delete
    suspend fun delete(sleepSegmentEventEntity: SleepSegmentEventEntity)

    @Query("DELETE FROM sleep_segment_events")
    suspend fun deleteAll()
}