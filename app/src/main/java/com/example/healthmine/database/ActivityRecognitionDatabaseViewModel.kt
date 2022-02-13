package com.example.healthmine.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.example.healthmine.models.AverageEgv
import kotlinx.coroutines.runBlocking
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityRecognitionDatabaseViewModel (private val repository: ActivityRecognitionRepository) : ViewModel() {
    val allWorkoutLiveData: LiveData<List<ActivityRecognitionEntity>> = repository.allWorkoutData.asLiveData()  //can also be implement in the repository

    //Used to insert a data row, it can also return a long value, which is the id
    fun insert(workout: ActivityRecognitionEntity) :Long{
        val workoutid = repository.insert(workout)
        return workoutid
    }
    //Get the duration from an interval
    fun getActivityDurationBetween(startTime: String, endTime: String): List<ActivityRecognitionEntity> {
        val actDuration: List<ActivityRecognitionEntity>
        runBlocking {
            actDuration = repository.getActivityDurationBetween(startTime, endTime)
        }
        return actDuration
    }

    fun getEarliestActivity(year:String): ActivityRecognitionEntity {
        val actDuration: ActivityRecognitionEntity
        runBlocking {
            actDuration = repository.getEarliestActivity(year)!!
        }
        return actDuration
    }

    fun getEarliestActivityWithoutYear(): ActivityRecognitionEntity {
        val actDuration: ActivityRecognitionEntity
        runBlocking {
            actDuration = repository.getEarliestActivityWithoutYear()
        }
        return actDuration
    }

    fun getLatestActivity(): ActivityRecognitionEntity{
        val actDuration: ActivityRecognitionEntity
        runBlocking {
            actDuration = repository.getLatestActivity()
        }
        return actDuration
    }

    fun getSingleDayActivityDuration(date: String): ActivityRecognitionEntity {
        val actDuration: ActivityRecognitionEntity
        runBlocking {
            actDuration = repository.getSingleDayActivityDuration(date)
        }
        return actDuration
    }

    fun checkExistanceOfTheDay(date: String): Int{
        val actDuration: Int
        runBlocking {
            actDuration = repository.checkExistanceOfTheDay(date)
        }
        return actDuration
    }

    fun updateStillDataOfToday(activity: Float, date: String){
        runBlocking {
            repository.updateStillDataOfToday(activity, date)
        }
    }

    fun updateRunDataOfToday(activity: Float, date: String){
        runBlocking {
            repository.updateRunDataOfToday(activity, date)
        }
    }

    fun updateWalkDataOfToday(activity: Float, date: String){
        runBlocking {
            repository.updateWalkDataOfToday(activity, date)
        }
    }

    fun updateVehicleDataOfToday(activity: Float, date: String){
        runBlocking {
            repository.updateVehicleDataOfToday(activity, date)
        }
    }

    fun updateBicycleDataOfToday(activity: Float, date: String){
        runBlocking {
            repository.updateBicycleDataOfToday(activity, date)
        }
    }

    fun updateOnFootDataOfToday(activity: Float, date: String){
        runBlocking {
            repository.updateOnFootDataOfToday(activity, date)
        }
    }

    fun updateTiltDataOfToday(activity: Float, date: String){
        runBlocking {
            repository.updateTiltDataOfToday(activity, date)
        }
    }

    fun updateUnknownDataOfToday(activity: Float, date: String){
        runBlocking {
            repository.updateUnknownDataOfToday(activity, date)
        }
    }


    //receive the id and delete the corresponding data row
    fun deleteOne(uid: String){
        val id = uid.toLong()
        repository.delete(id)
    }

    //The following two functions are not been used
    fun deleteAll(){
        val workoutList = allWorkoutLiveData.value //global
        if (workoutList != null && workoutList.size > 0)  //basically don;t need this
            repository.deleteAll()
    }
}

//Define a viewmodel factory class
class ManualInputDataViewModelFactory (private val repository: ActivityRecognitionRepository) : ViewModelProvider.Factory {
    override fun<T: ViewModel> create(modelClass: Class<T>) : T{ //create() creates a new instance of the modelClass, which is CommentViewModel in this case.
        if(modelClass.isAssignableFrom(ActivityRecognitionDatabaseViewModel::class.java))  //Use the viewmodelfactory to tell is the created modelclass the commentview model we created, or some other database
            return ActivityRecognitionDatabaseViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}