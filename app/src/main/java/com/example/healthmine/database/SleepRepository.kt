package com.example.healthmine.database

import com.example.healthmine.models.SleepClassifyEventEntity
import com.example.healthmine.models.SleepSegmentEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

class SleepRepository(
    private val sleepSegmentEventDao: SleepSegmentEventDao,
    private val sleepClassifyEventDao: SleepClassifyEventDao
) {

    // Methods for SleepSegmentEventDao
    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allSleepSegmentEvents: Flow<List<SleepSegmentEventEntity>> =
        sleepSegmentEventDao.getAll()

    // By default Room runs suspend queries off the main thread. Therefore, we don't need to
    // implement anything else to ensure we're not doing long-running database work off the
    // main thread.
    suspend fun insertSleepSegment(sleepSegmentEventEntity: SleepSegmentEventEntity) {
        sleepSegmentEventDao.insert(sleepSegmentEventEntity)
    }

    // By default Room runs suspend queries off the main thread. Therefore, we don't need to
    // implement anything else to ensure we're not doing long-running database work off the
    // main thread.
    suspend fun insertSleepSegments(sleepSegmentEventEntities: List<SleepSegmentEventEntity>) {
        sleepSegmentEventDao.insertAll(sleepSegmentEventEntities)
    }

    suspend fun getSleepSegmentEndBetween(startTime: ZonedDateTime, endTime: ZonedDateTime): List<SleepSegmentEventEntity> {
        return sleepSegmentEventDao.getSleepSegmentEndBetween(startTime, endTime)
    }

    fun getMonthlySleepSegmentStartBetween(startZonedDateTime: ZonedDateTime, endZonedDateTime: ZonedDateTime): List<Float> {
        var startTime = startZonedDateTime
        var endTime = endZonedDateTime
        val results = ArrayList<Float>()
        while (startTime.isBefore(endTime)) {

            val endOfMonth = startTime.plusMonths(1)
            endTime = if (endOfMonth.isBefore(endTime)) {
                endOfMonth
            } else {
                endTime
            }

            var result = 0.0f

            val segments: List<SleepSegmentEventEntity>
            runBlocking {
                segments = sleepSegmentEventDao.getSleepSegmentEndBetween(startTime, endTime)
            }
            segments.forEach {
                val duration = Duration.between(it.startTime, it.endTime).toMillis() / (1000 * 3600)
                result += duration
            }

            if (segments.isNullOrEmpty()) {
                result = 0.0f
            } else {
                result /= segments.size
            }

            results.add(result)

            startTime = startTime.plusMonths(1)
        }
        return results
    }

    suspend fun deleteAllSegments() {
        sleepSegmentEventDao.deleteAll()
    }

    suspend fun getSleepClassifyBetween(startTime: Int, endTime: Int): List<SleepClassifyEventEntity> {
        return sleepClassifyEventDao.getSleepClassifyBetween(startTime, endTime)
    }

    // Methods for SleepClassifyEventDao
    // Observed Flow will notify the observer when the data has changed.
    val allSleepClassifyEvents: Flow<List<SleepClassifyEventEntity>> =
        sleepClassifyEventDao.getAll()

    suspend fun insertSleepClassifyEvent(sleepClassifyEventEntity: SleepClassifyEventEntity) {
        sleepClassifyEventDao.insert(sleepClassifyEventEntity)
    }

    suspend fun insertSleepClassifyEvents(sleepClassifyEventEntities: List<SleepClassifyEventEntity>) {
        sleepClassifyEventDao.insertAll(sleepClassifyEventEntities)
    }
}