package com.example.healthmine.ui.firebase

import android.app.Activity
import android.util.Log
import android.widget.TextView
import com.example.healthmine.database.*
import com.example.healthmine.utils.FirebaseDatabaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.database.DatabaseError

import com.google.firebase.database.DataSnapshot

import com.google.firebase.database.ValueEventListener


object FirebaseUtil {
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference:DatabaseReference
    private lateinit var activityRecognitionDatabase: HealthmineDatabase
    private lateinit var activityRecognitionDatabaseDao: ActivityRecognitionDao
    private lateinit var repository: ActivityRecognitionRepository

    //write to database when first registered
    fun firstTimeToDatabase(activity: Activity, name:String, email:String){
        activityRecognitionDatabase = HealthmineDatabase.getInstance(activity)  //Create the database instance
        activityRecognitionDatabaseDao = activityRecognitionDatabase.activityDatabaseDao //Connect the dao with the database
        repository = ActivityRecognitionRepository(activityRecognitionDatabaseDao) //Connect the repository with the dao
        try{
            repository.deleteAll()
        }
        catch (e:Exception){
            println("No database to delete")
        }

        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            FirebaseDatabaseUtil.firebaseDatabase.getReference(user.uid).child("settings")
        }

        val user = User(name, email)
        myRef?.setValue(user)
    }

    //upload activity data stored in the local database as a list to the database
    //call the function and pass in activity to sync to google firebase database
    suspend fun uploadDataToFirebase(activity: Activity){
        activityRecognitionDatabase = HealthmineDatabase.getInstance(activity)
        activityRecognitionDatabaseDao = activityRecognitionDatabase.activityDatabaseDao

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")
        println("Test: UID is $uid")

        activityRecognitionDatabase?.let { db ->
            withContext(Dispatchers.IO) {
//                FirebaseDatabaseUtil.generateId("activityList")
                val entries = readEntriesFromActivities(activity)
                val entriesList = ArrayList<ActivityRecognitionObject>()
                if (entries != null) {
                    for (entry in entries){
                        val activityRecognitionObject = entryToDataObject(entry)
                        entriesList.add(activityRecognitionObject)
                    }
                    if (uid != null && entriesList.size>0){
                        println("Test: entriesList is $entriesList")
                        //FirebaseDatabaseUtil.save("activityList", entriesList)
                        databaseReference.child(uid).child("activityList").setValue(entriesList)
                    }
                }
            }
        }
    }

    //update the name in the database
    fun updateNameToDatabase(name:String){
        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            FirebaseDatabaseUtil.firebaseDatabase.getReference(user.uid).child("settings")
        }

        myRef?.child("name")?.setValue(name)
    }

    //read the name stored in the firebase to local
    fun readNameFromFirebase(textView:TextView){
        var returnName: String
        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            FirebaseDatabaseUtil.firebaseDatabase.getReference(user.uid).child("settings")
        }

        myRef?.child("name")?.get()?.addOnSuccessListener {
            returnName = it.value.toString()
            textView.text = returnName
        }?.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }
    }

    //read email address stored in the database to local
    fun readEmailFromFirebase(textView:TextView){
        var returnName: String
        val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
            FirebaseDatabaseUtil.firebaseDatabase.getReference(user.uid).child("settings")
        }

        myRef?.child("email")?.get()?.addOnSuccessListener {
            returnName = it.value.toString()
            textView.text = returnName
        }?.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }
    }

    //read activity list data from database and write them to local database
    // this is called when the login user is different from previous user
    suspend fun readDataFromFirebase(activity: Activity){
        activityRecognitionDatabase = HealthmineDatabase.getInstance(activity)
        activityRecognitionDatabaseDao = activityRecognitionDatabase.activityDatabaseDao
        repository = ActivityRecognitionRepository(activityRecognitionDatabaseDao)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")

        activityRecognitionDatabase?.let { db ->
            withContext(Dispatchers.IO) {
                if (uid != null){
                    //reference: https://stackoverflow.com/questions/40366717/firebase-for-android-how-can-i-loop-through-a-child-for-each-child-x-do-y
                    databaseReference.child(uid).child("activityList")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                repository.deleteAll()
                                for (snapshot in dataSnapshot.children) {
                                    val user = snapshot.getValue(
                                        ActivityRecognitionObject::class.java
                                    )

                                    var entry = user?.let { convertActivityEntryToEntry(it) }
                                    if (entry != null) {
                                        repository.insert(entry)
                                    }
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {}
                        })
                }
            }
        }
    }

    // convert activity object retrieved from firebase to a ActivityRecognitionEntity
    private fun convertActivityEntryToEntry(entry: ActivityRecognitionObject): ActivityRecognitionEntity{
        var returnVal = ActivityRecognitionEntity(
            entry.sequencedId,
            entry.id,
            entry.Date?:"",
            entry.StillTime,
            entry.WalkTime,
            entry.RunTime,
            entry.VehicleTime,
            entry.BicycleTime,
            entry.OnFootTime,
            entry.TiltTime,
            entry.UnknownTime
        )
        return returnVal
    }

    //read activity data from local database
    //returns a list of ActivityRecognitionEntity
    private suspend fun readEntriesFromActivities(activity: Activity): List<ActivityRecognitionEntity>? {
        activityRecognitionDatabase =
            HealthmineDatabase.getInstance(activity)  //Create the database instance
        activityRecognitionDatabaseDao =
            activityRecognitionDatabase.activityDatabaseDao //Connect the dao with the database
        return activityRecognitionDatabaseDao.getListOfData()
    }

    //helper function that converts entry to an object that will be uploaded to the firebase
    private fun entryToDataObject(entry:ActivityRecognitionEntity):ActivityRecognitionObject{
        var returnVal = ActivityRecognitionObject(
            entry.sequenceId,
            entry.id,
            entry.Date,
            entry.StillTime,
            entry.WalkTime,
            entry.RunTime,
            entry.VehicleTime,
            entry.BicycleTime,
            entry.OnFootTime,
            entry.TiltTime,
            entry.UnknownTime
        )
        return returnVal
    }

}

//the object that will be uploaded to firebase for each user
data class User(
    var name:String?="",
    var email:String?="",
)

//the object class that are in the activityList in the User object
data class ActivityRecognitionObject(
    var sequencedId: String = "",
    var id: Long = 0L,
    var Date: String = "",
    var StillTime: Float = 0F,
    var WalkTime: Float = 0F,
    var RunTime: Float = 0F,
    var VehicleTime: Float = 0F,
    var BicycleTime: Float = 0F,
    var OnFootTime: Float = 0F,
    var TiltTime: Float = 0F,
    var UnknownTime: Float = 0F,
)
