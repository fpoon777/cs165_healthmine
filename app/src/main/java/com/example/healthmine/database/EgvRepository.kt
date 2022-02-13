package com.example.healthmine.database

import com.example.healthmine.models.AverageEgv
import com.example.healthmine.models.EGV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDateTime


class EgvRepository(private val egvDao: EgvDao) {

    suspend fun insertAllAverageEgvs(averageEgvs: List<AverageEgv>) {
        egvDao.insertAllAverageEgvs(averageEgvs)
    }

    suspend fun getAvgEgvsBetween(tag: Int, startTime: LocalDateTime, endTime: LocalDateTime): List<AverageEgv> {
        return egvDao.getAvgEgvsBetween(tag, startTime, endTime)
    }

    suspend fun getEarliestAverageEvgByTag(tag: Int): AverageEgv? {
        return egvDao.getEarliestAverageEgvByTag(tag)
    }

    suspend fun getLatestAverageEgvByTag(tag: Int): AverageEgv? {
        return egvDao.getLatestAverageEgvByTag(tag)
    }

    fun updateAverageEgv(averageEgv: AverageEgv) {
        CoroutineScope(IO).launch {
            egvDao.updateAverageEgv(averageEgv)
        }
    }

//    fun deleteAverageEgvById(id: Long) {
//        CoroutineScope(IO).launch {
//            egvDao.deleteAverageEgvById(id)
//        }
//    }
}