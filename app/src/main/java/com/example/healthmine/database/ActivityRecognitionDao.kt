package com.example.healthmine.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.healthmine.models.AverageEgv
import com.example.healthmine.models.SleepSegmentEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime


@Dao
interface ActivityRecognitionDao {

    @Insert
    suspend fun insertWorkoutData(workout: ActivityRecognitionEntity): Long //most of them will have suspend
    //Set a return value in long form to return the id of a dataset

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workout: List<ActivityRecognitionEntity>)

    @Query("SELECT * FROM activity_recognition_time_duration_data_table")
    fun getAllWorkoutData(): Flow<List<ActivityRecognitionEntity>>  //update the list automatically, use the magic "Flow". CHOOSE THE COROUTINE/KOTLIN

    @Query("DELETE FROM activity_recognition_time_duration_data_table") //not be used in this project
    suspend fun deleteAll()

    @Query("DELETE FROM activity_recognition_time_duration_data_table WHERE id = :key") //":" indicates that it is a Bind variable
    suspend fun deleteOne(key: Long)


    //added for firebase read
    @Query("SELECT * FROM activity_recognition_time_duration_data_table")
    suspend fun getListOfData(): List<ActivityRecognitionEntity>?

    @Query("SELECT * FROM activity_recognition_time_duration_data_table WHERE activity_date BETWEEN :startTime AND :endTime")
    suspend fun getActivityDurationBetween(startTime: String, endTime: String): List<ActivityRecognitionEntity>

    @Query("SELECT * FROM activity_recognition_time_duration_data_table WHERE activity_date LIKE :year ORDER BY activity_date LIMIT 1")
    suspend fun getEarliestActivity(year: String): ActivityRecognitionEntity

    @Query("SELECT * FROM activity_recognition_time_duration_data_table WHERE activity_date = :date")
    suspend fun getSingleDayActivityDuration(date: String): ActivityRecognitionEntity

    @Query("SELECT * FROM activity_recognition_time_duration_data_table ORDER BY activity_date DESC LIMIT 1")
    suspend fun getLatestActivity(): ActivityRecognitionEntity

    @Query("SELECT * FROM activity_recognition_time_duration_data_table ORDER BY activity_date LIMIT 1")
    suspend fun getEarliestActivityWithoutYear(): ActivityRecognitionEntity

    @Query("SELECT COUNT(*) FROM activity_recognition_time_duration_data_table WHERE activity_date LIKE :date LIMIT 1")
    suspend fun checkExistanceOfTheDay(date: String): Int

    @Query("UPDATE activity_recognition_time_duration_data_table SET still_activity_recognition_time_duration =  :still WHERE activity_date = :date")
    suspend fun updateStillDataOfToday(still:Float, date: String)

    @Query("UPDATE activity_recognition_time_duration_data_table SET walk_activity_recognition_time_duration =  :walk WHERE activity_date = :date")
    suspend fun updateWalkDataOfToday(walk:Float, date: String)

    @Query("UPDATE activity_recognition_time_duration_data_table SET run_activity_recognition_time_duration =  :run WHERE activity_date = :date")
    suspend fun updateRunDataOfToday(run:Float, date: String)

    @Query("UPDATE activity_recognition_time_duration_data_table SET vehicle_activity_recognition_time_duration =  :vehicle WHERE activity_date = :date")
    suspend fun updateVehicleDataOfToday(vehicle:Float, date: String)

    @Query("UPDATE activity_recognition_time_duration_data_table SET bicycle_activity_recognition_time_duration =  :bicycle WHERE activity_date = :date")
    suspend fun updateBicycleDataOfToday(bicycle:Float, date: String)

    @Query("UPDATE activity_recognition_time_duration_data_table SET on_foot_activity_recognition_time_duration =  :on_foot WHERE activity_date = :date")
    suspend fun updateOnFootDataOfToday(on_foot:Float, date: String)

    @Query("UPDATE activity_recognition_time_duration_data_table SET tilt_activity_recognition_time_duration = :tilt WHERE activity_date = :date")
    suspend fun updateTiltDataOfToday(tilt:Float, date: String)

    @Query("UPDATE activity_recognition_time_duration_data_table SET unknown_activity_recognition_time_duration = :unknown WHERE activity_date = :date")
    suspend fun updateUnknownDataOfToday(unknown:Float, date: String)

}
