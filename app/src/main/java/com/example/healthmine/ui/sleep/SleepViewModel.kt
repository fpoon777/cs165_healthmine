package com.example.healthmine.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.healthmine.database.SleepRepository
import com.example.healthmine.models.SleepClassifyEventEntity
import com.example.healthmine.models.SleepSegmentEventEntity
import kotlinx.coroutines.runBlocking
import java.lang.IllegalArgumentException
import java.time.ZonedDateTime

class SleepViewModel(private val repository: SleepRepository): ViewModel() {
    fun getSleepSegmentEndBetween(startTime: ZonedDateTime, endTime: ZonedDateTime): List<SleepSegmentEventEntity> {
        val sleepSegments: List<SleepSegmentEventEntity>
        runBlocking {
            sleepSegments = repository.getSleepSegmentEndBetween(startTime, endTime)
        }
        return sleepSegments
    }

    fun getMonthlySleepSegments(startTime: ZonedDateTime, endTime: ZonedDateTime): List<Float> {
        return repository.getMonthlySleepSegmentStartBetween(startTime, endTime)
    }

    fun getSleepClassifyBetween(startTime: Int, endTime: Int): List<SleepClassifyEventEntity> {
        val sleepClassifyEvents: List<SleepClassifyEventEntity>
        runBlocking {
            sleepClassifyEvents = repository.getSleepClassifyBetween(startTime, endTime)
        }
        return sleepClassifyEvents
    }
}

class SleepViewModelFactory(private val repository: SleepRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SleepViewModel::class.java)) {
            return SleepViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}