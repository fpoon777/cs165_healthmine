package com.example.healthmine.ui.self_report.self_report_db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SelfReportDbDao {
    @Insert
    suspend fun insertExerciseEntry(selfReportEntry: SelfReportEntry)

    @Query("SELECT * FROM self_report_table")
    fun getAllExerciseEntries(): Flow<List<SelfReportEntry>>

    @Query("DELETE FROM self_report_table")
    suspend fun deleteAll()

    @Query("DELETE FROM self_report_table WHERE id=:id")
    suspend fun deleteExerciseEntry(id:Long)

    @Query("SELECT * FROM self_report_table WHERE id=:id")
    fun getItemById(id:Long):Flow<SelfReportEntry>
}