package com.example.healthmine.database

import androidx.room.*
import com.example.healthmine.models.SleepClassifyEventEntity
import com.example.healthmine.models.SleepSegmentEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepClassifyEventDao {
    @Query("SELECT * FROM sleep_classify_events ORDER BY timestampSeconds DESC")
    fun getAll(): Flow<List<SleepClassifyEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sleepClassifyEventEntity: SleepClassifyEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sleepClassifyEventEntities: List<SleepClassifyEventEntity>)

    @Query("SELECT * FROM sleep_classify_events WHERE timestampSeconds BETWEEN :startTime AND :endTime")
    suspend fun getSleepClassifyBetween(startTime: Int, endTime: Int): List<SleepClassifyEventEntity>

    @Delete
    suspend fun delete(sleepClassifyEventEntity: SleepClassifyEventEntity)

    @Query("DELETE FROM sleep_classify_events")
    suspend fun deleteAll()
}