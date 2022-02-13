package com.example.healthmine.utils

import android.content.Context
import com.example.healthmine.database.*
import com.example.healthmine.models.SleepSegmentEventDTO
import com.example.healthmine.models.SleepSegmentEventEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


object FirebaseDatabaseUtil {
    val firebaseDatabase = Firebase.database

    fun generateId(path: String): DatabaseReference? {
        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            firebaseDatabase.getReference(user.uid).child(path)
        }
        return myRef?.push()
    }

    fun <T> save(path: String, t: T) {
        // Write a message to the database
        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            firebaseDatabase.getReference(user.uid).child(path)
        }

        myRef?.push()?.setValue(t)
    }

    fun <T> saveWithId(path: String, pushKey: String, t: T) {
        // Write a message to the database
        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            firebaseDatabase.getReference(user.uid).child(path).child(pushKey)
        }

        myRef?.setValue(t)
    }

    fun downloadSleepData(context: Context) {
        val database = HealthmineDatabase.getInstance(context)
        val databaseDao = database.sleepSegmentEventDao
        val dao = database.sleepClassifyEventDao
        val repository = SleepRepository(databaseDao, dao)

        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            firebaseDatabase.getReference(user.uid).child("sleep")
        }
        myRef?.orderByChild(SleepSegmentEventEntity::startTime.name)
            ?.get()
            ?.addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.value != null) {
                    val result = dataSnapshot.children.map {
                        val item = it.getValue<SleepSegmentEventDTO>()
                        SleepSegmentEventEntity.fromDTO(it.key, item!!)
                    }
                    CoroutineScope(IO).launch {
                        repository.insertSleepSegments(result)
                    }
                }
                println("debug: firebase " + "Got value ${dataSnapshot.value}")
            }?.addOnFailureListener{
                println("debug: firebase Error getting data $it")
            }
    }

    fun downloadActivityTimestampData(context: Context) {
        val database = HealthmineDatabase.getInstance(context)
        val databaseDao = database.activityDatabaseDao
//        val dao = database.sleepClassifyEventDao
        val repository = ActivityRecognitionRepository(databaseDao)

        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            firebaseDatabase.getReference(user.uid).child("activityRecognition")
        }
        myRef?.orderByChild("date")
            ?.get()
            ?.addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.value != null) {
                    val result = dataSnapshot.children.map {
                        val stand = it.child("stillTime").getValue<Float>()
                        val walk = it.child("walkTime").getValue<Float>()
                        val run = it.child("runTime").getValue<Float>()
                        val bicycle = it.child("bicycleTime").getValue<Float>()
                        val vehicle = it.child("vehicleTime").getValue<Float>()
                        val foot = it.child("onFootTime").getValue<Float>()
                        val tilt = it.child("tiltTime").getValue<Float>()
                        val unknown = it.child("unknownTime").getValue<Float>()
                        val seq = it.child("sequenceId").getValue<String>()
                        val day = it.child("date").getValue<String>()
                        println("Peek: stand time is $stand")
                        ActivityRecognitionEntity.from(it.key!!, day!!, stand!!, walk!!, run!!, vehicle!!, bicycle!!, foot!!,tilt!!,unknown!!)
                    }
                    CoroutineScope(IO).launch {
                        repository.insertAll(result)
                    }
                }
                println("debug: firebase " + "Got value ${dataSnapshot.value}")
            }?.addOnFailureListener{
                println("debug: firebase Error getting data $it")
            }
    }

    fun isExisted(path: String, primaryKey: String, value: Long): Boolean {
        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            firebaseDatabase.getReference(user.uid).child(path)
        }
        var result = false
        runBlocking {
            myRef?.orderByChild(primaryKey)?.equalTo(value.toDouble())?.get()?.addOnSuccessListener {
                if (it.value != null) {
                    result = true
                }
                println("debugfirebase: " + "Got value ${it.value}")
            }?.addOnFailureListener{
                println("debugfirebase: " + "Error getting data ${it}")
            }
        }

        println("debugfirebase: $result")
        return result
    }

    fun getSleepSegmentStartBetween(startTime: Long,
                                    endTime: Long,
                                    callback: (it: List<SleepSegmentEventEntity>) -> Unit) {
        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            firebaseDatabase.getReference(user.uid).child("sleep")
        }
        myRef?.orderByChild(SleepSegmentEventEntity::startTime.name)
            ?.startAt(startTime.toDouble())?.endBefore(endTime.toDouble())?.get()
            ?.addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot != null) {
                    val result = dataSnapshot.children.map {
                        val item = it.getValue<SleepSegmentEventEntity>()
                        SleepSegmentEventEntity(null, item!!.startTime, item.endTime, item.status)

                    }
                    println("debug: sleep firebase ${result.size}")
                    callback(result)
                }

                println("debug: firebase " + "Got value ${dataSnapshot.value}")
            }?.addOnFailureListener{
                println("debug: firebase Error getting data $it")
            }
    }
}