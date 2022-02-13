package com.example.healthmine.database

import androidx.lifecycle.LiveData
import com.example.healthmine.models.AverageEgv
import com.example.healthmine.models.SleepSegmentEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityRecognitionRepository (private val activityDatabaseDao: ActivityRecognitionDao) {

    val allWorkoutData: Flow<List<ActivityRecognitionEntity>> = activityDatabaseDao.getAllWorkoutData() //get updated auto

    fun insert(workout: ActivityRecognitionEntity): Long {
//        CoroutineScope(Dispatchers.IO).launch() {
//            val workoutid = commentDatabaseDao.insertWorkoutData(workout)  //make sure to use coroutine, using the worker thread to insert a comment into a database
//        }
        var id:Long = 0L
        //use runBlocking to pass the user id
        runBlocking(Dispatchers.IO) {
            id = activityDatabaseDao.insertWorkoutData(workout) //not using coroutine anymore but still on worker thread
        }
        return id
    }
    suspend fun insertAll(workout: List<ActivityRecognitionEntity>) {
        activityDatabaseDao.insertAll(workout)
    }
    suspend fun getActivityDurationBetween(startTime:String, endTime: String): List<ActivityRecognitionEntity> {
        return activityDatabaseDao.getActivityDurationBetween(startTime, endTime)
    }

    suspend fun getEarliestActivity(year:String): ActivityRecognitionEntity {
        return activityDatabaseDao.getEarliestActivity(year)!!
    }

    fun getSingleDayActivityDuration(date:String): ActivityRecognitionEntity {
        var result: ActivityRecognitionEntity
        runBlocking(Dispatchers.IO) {
             result = activityDatabaseDao.getSingleDayActivityDuration(date)
        }
        return result
    }

    suspend fun getLatestActivity(): ActivityRecognitionEntity {
        return activityDatabaseDao.getLatestActivity()
    }
    suspend fun getEarliestActivityWithoutYear(): ActivityRecognitionEntity {
        return activityDatabaseDao.getEarliestActivityWithoutYear()
    }

    fun checkExistanceOfTheDay(date: String): Int {
        var id: Int = 0
        runBlocking(Dispatchers.IO) {
            id  = activityDatabaseDao.checkExistanceOfTheDay(date)
        }
        return id
    }

    fun updateStillDataOfToday(activity: Float, date: String){
        runBlocking(Dispatchers.IO) {
            activityDatabaseDao.updateStillDataOfToday(activity, date)
        }
    }

    fun updateWalkDataOfToday(activity: Float, date: String){
        runBlocking(Dispatchers.IO) {
            activityDatabaseDao.updateWalkDataOfToday(activity, date)
        }
    }

    fun updateRunDataOfToday(activity: Float, date: String){
        runBlocking(Dispatchers.IO) {
            activityDatabaseDao.updateRunDataOfToday(activity, date)
        }
    }

    fun updateVehicleDataOfToday(activity: Float, date: String){
        runBlocking(Dispatchers.IO) {
            activityDatabaseDao.updateVehicleDataOfToday(activity, date)
        }
    }

    fun updateBicycleDataOfToday(activity: Float, date: String){
        runBlocking(Dispatchers.IO) {
            activityDatabaseDao.updateBicycleDataOfToday(activity, date)
        }
    }

    fun updateOnFootDataOfToday(activity: Float, date: String){
        runBlocking(Dispatchers.IO) {
            activityDatabaseDao.updateOnFootDataOfToday(activity, date)
        }
    }

    fun updateTiltDataOfToday(activity: Float, date: String){
        runBlocking(Dispatchers.IO) {
            activityDatabaseDao.updateTiltDataOfToday(activity, date)
        }
    }

    fun updateUnknownDataOfToday(activity: Float, date: String){
        runBlocking(Dispatchers.IO) {
            activityDatabaseDao.updateUnknownDataOfToday(activity, date)
        }
    }


    //Connect with the deleteOne function in the dao
    fun delete(id: Long){
        CoroutineScope(Dispatchers.IO).launch {
            activityDatabaseDao.deleteOne(id)   //basically, dao helps you to implement the sql code
        }
    }

    //The following two functions are not been used in this project
    fun deleteAll(){
        CoroutineScope(Dispatchers.IO).launch {
            activityDatabaseDao.deleteAll()
        }
    }

}