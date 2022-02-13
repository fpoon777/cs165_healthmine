package com.example.healthmine.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.healthmine.models.AverageEgv
import com.example.healthmine.models.EGV
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface EgvDao {

    @Insert
    suspend fun insertAllAverageEgvs(averageEgvs: List<AverageEgv>)

    @Query("SELECT * FROM average_egvs WHERE tag = :tag AND systemTime BETWEEN :startTime AND :endTime")
    suspend fun getAvgEgvsBetween(tag: Int, startTime: LocalDateTime, endTime: LocalDateTime): List<AverageEgv>

    @Query("SELECT * FROM average_egvs WHERE tag = :tag ORDER BY systemTime LIMIT 1")
    suspend fun getEarliestAverageEgvByTag(tag: Int): AverageEgv?

    @Query("SELECT * FROM average_egvs WHERE tag = :tag ORDER BY systemTime DESC LIMIT 1")
    suspend fun getLatestAverageEgvByTag(tag: Int): AverageEgv?

    @Update
    suspend fun updateAverageEgv(averageEgv: AverageEgv)

//    @Query("DELETE FROM average_egvs WHERE id = :id")
//    suspend fun deleteAverageEgvById(id: Long)

}