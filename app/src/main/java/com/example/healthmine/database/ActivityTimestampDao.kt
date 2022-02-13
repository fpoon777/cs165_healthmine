package com.example.healthmine.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface ActivityTimestampDao {

    @Insert
    suspend fun insertActivityTimestamp(activityTimetamp: ActivityTimestampEntity) //most of them will have suspend
    //Set a return value in long form to return the id of a dataset

    @Query("SELECT * FROM activity_timestamp_table")
    fun getAllActivityTimestampData(): Flow<List<ActivityTimestampEntity>>  //update the list automatically, use the magic "Flow". CHOOSE THE COROUTINE/KOTLIN

    //add a value to timestamps
    @Query("UPDATE activity_timestamp_table SET timestamps = timestamps ||',' || :new_timestamp")
    suspend fun addTimestamp(new_timestamp: String)

    //add a value to activity type
    @Query("UPDATE activity_timestamp_table SET activity_type = activity_type ||',' || :new_type")
    suspend fun addActivityType(new_type: String)

    //added for firebase read
    @Query("SELECT * FROM activity_timestamp_table")
    suspend fun getListOfData(): List<ActivityTimestampEntity>?

//    @Query("SELECT COUNT(*) FROM activity_timestamp_table WHERE activity_date LIKE :date LIMIT 1")
//    suspend fun checkExistanceOfTheDay(date: String): Int

    @Query("DELETE FROM activity_timestamp_table")
    suspend fun deleteAll()

}