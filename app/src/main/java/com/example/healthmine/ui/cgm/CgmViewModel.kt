package com.example.healthmine.ui.cgm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.example.healthmine.database.EgvRepository
import com.example.healthmine.models.AverageEgv
import com.example.healthmine.models.EGV
import kotlinx.coroutines.runBlocking
import java.lang.IllegalArgumentException
import java.time.LocalDateTime

class CgmViewModel(private val repository: EgvRepository): ViewModel() {
    fun getAvgEgvsBetween(tag: Int, startTime: LocalDateTime, endTime: LocalDateTime): List<AverageEgv> {
        val egvs: List<AverageEgv>
        runBlocking {
            egvs = repository.getAvgEgvsBetween(tag, startTime, endTime)
        }
        return egvs
    }
}

class CgmViewModelFactory(private val repository: EgvRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CgmViewModel::class.java)) {
            return CgmViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}