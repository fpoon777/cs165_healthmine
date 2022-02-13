package com.example.healthmine.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

class ActivityTimestampRepository (private val activityTimestampDao: ActivityTimestampDao) {

    val allWorkoutData: Flow<List<ActivityTimestampEntity>> = activityTimestampDao.getAllActivityTimestampData()

    fun insert(activityTimestamp: ActivityTimestampEntity) {
        //var id:Long = 0L
        //use runBlocking to pass the user id
        runBlocking(Dispatchers.IO) {
            activityTimestampDao.insertActivityTimestamp(activityTimestamp) //not using coroutine anymore but still on worker thread
        }
//        return id
    }

    fun addTimestamp(new_timestamp: String){
        runBlocking(Dispatchers.IO) {
            activityTimestampDao.addTimestamp(new_timestamp)
        }
    }

    fun addActivityType(new_type: String){
        runBlocking(Dispatchers.IO) {
            activityTimestampDao.addActivityType(new_type)
        }
    }

    fun getListOfData(){
        runBlocking(Dispatchers.IO) {
            activityTimestampDao.getListOfData()
        }
    }

//    fun checkExistanceOfTheDay(date: String): Int {
//        var id: Int = 0
//        runBlocking(Dispatchers.IO) {
//            id  = activityTimestampDao.checkExistanceOfTheDay(date)
//        }
//        return id
//    }

}