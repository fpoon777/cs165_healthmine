package com.example.healthmine.ui.activityrecognition

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.healthmine.database.*
import com.example.healthmine.ui.activityrecognition.ActivityRecognition.Companion.BROADCAST_DETECTED_ACTIVITY
import com.example.healthmine.ui.activityrecognition.ActivityRecognition.Companion.DETECTION_INTERVAL_IN_MILLISECONDS
import com.example.healthmine.ui.firebase.FirebaseUtil
import com.example.healthmine.ui.sleep.SleepReceiver
import com.example.healthmine.utils.DexcomUtil
import com.example.healthmine.utils.FirebaseDatabaseUtil
import com.example.healthmine.utils.UnauthorizedException
import com.google.android.gms.location.*
import com.google.android.gms.location.ActivityRecognition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import com.google.firebase.database.DatabaseError

import androidx.annotation.NonNull

import com.google.android.gms.tasks.OnSuccessListener

import com.google.firebase.database.DataSnapshot

import com.google.firebase.database.ValueEventListener





class BackgroundDetectedActivitiesService : Service() {
    private val TAGSERVICE = ActivityRecognition::class.java.simpleName
    private lateinit var mIntentService: Intent
    private lateinit var mPendingIntent: PendingIntent
    private lateinit var mActivityRecognitionClient: ActivityRecognitionClient
    internal lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var timer: Timer
    private var counter = 0
    private var msgHandler: Handler? = null
    private val scope: CoroutineScope = MainScope()

    //Define the database
    lateinit var activityRecognitionDatabase: HealthmineDatabase
    lateinit var activityRecognitionDatabaseDao: ActivityRecognitionDao
    lateinit var activityTimestampDatabase: HealthmineDatabase
    lateinit var activityTimestampDao: ActivityTimestampDao
    lateinit var repository: ActivityRecognitionRepository
    lateinit var timestampRepository: ActivityTimestampRepository
    lateinit var viewModelFactory: ManualInputDataViewModelFactory
    lateinit var activityRecognitionDataViewModel: ActivityRecognitionDatabaseViewModel

    //Different method to set binder
    internal var mBinder: IBinder = LocalBinder()
    inner class LocalBinder : Binder() {
        val serverInstance: BackgroundDetectedActivitiesService
            get() = this@BackgroundDetectedActivitiesService

        fun setmsgHandler(msgHandler: Handler) {
            this@BackgroundDetectedActivitiesService.msgHandler = msgHandler
        }
    }
    companion object{
        val ACTIVITY_STILL_KEY = "activity still key"
        val ACTIVITY_WALK_KEY = "activity walk key"
        val ACTIVITY_RUN_KEY = "activity run key"
        val ACTIVITY_FOOT_KEY = "activity on foot key"
        val ACTIVITY_VEHICLE_KEY = "activity vehicle key"
        val ACTIVITY_BICYCLE_KEY = "activity bicycle key"
        val ACTIVITY_TILT_KEY = "activity tilt key"
        val ACTIVITY_UNKNOWN_KEY = "activity unknown key"
        val ACTIVITY_OBSERVER_KEY = "observe"
        val ACTIVITY_RECOGNITION_MSG = 0
        private val TAG = BackgroundDetectedActivitiesService::class.java?.getSimpleName()
    }

    //Set a temperal data list
    private var standStillTime: Float = 0F
    private var walkTime: Float = 0F
    private var vehicleTime: Float = 0F
    private var runningTime: Float = 0F
    private var bicycleTime: Float = 0F
    private var onFootTime:Float = 0F
    private var tiltTime: Float = 0F
    private var unknownTime:Float = 0F

    private var tempType: Int = 0
    private var innertype: Int = 0
    private var innerconfidence: Int = 0

    //Timestamps
    private lateinit var startTimestamp: Calendar
    private lateinit var tempTimestamp: Calendar
    private lateinit var endTimestamp: Calendar
    private var enterFlag = 0

    val mainHandler = Handler(Looper.getMainLooper())

    //Data saving indicator
    private lateinit var nowDate: LocalDate
    private lateinit var previousDate: LocalDate
    private var dateIndicator: Int = 0
    private lateinit var nowDateTime: LocalDateTime
    private lateinit var anticipateTime: LocalDateTime

    private var templabel: Int = 3

//    sleep api
    private lateinit var sleepPendingIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()
        mActivityRecognitionClient = ActivityRecognitionClient(this)
        mIntentService = Intent(this, DetectedActivitiesIntentService::class.java)
        mPendingIntent = PendingIntent.getService(this, 1, mIntentService, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        requestActivityUpdatesButtonHandler()
        startTimestamp = Calendar.getInstance()

        println("The service got started")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BROADCAST_DETECTED_ACTIVITY) {
                    val type = intent.getIntExtra("type", -1)
                    val confidence = intent.getIntExtra("confidence", 0)
                    handleUserActivity(type, confidence)
                    println("The detected activity type is $type")
                    PassActivityInfo() //Pass the data to Viewmodel instantly
                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
            IntentFilter(BROADCAST_DETECTED_ACTIVITY)
        )

        activityRecognitionDatabase = HealthmineDatabase.getInstance(this)  //Create the database instance
        activityRecognitionDatabaseDao = activityRecognitionDatabase.activityDatabaseDao //Connect the dao with the database
        repository = ActivityRecognitionRepository(activityRecognitionDatabaseDao) //Connect the repository with the dao

        activityTimestampDatabase = HealthmineDatabase.getInstance(this)  //Create the database instance
        activityTimestampDao = activityTimestampDatabase.activityTimestampDao //Connect the dao with the database
        timestampRepository = ActivityTimestampRepository(activityTimestampDao) //Connect the repository with the dao


        //Save the data at 23:59PM
        if(dateIndicator == 0){
            dateIndicator = 1
            nowDateTime = LocalDateTime.now()
            //anticipateTime = LocalDate.now().atTime(23,59)
            //val minutes: Long = ChronoUnit.MINUTES.between(nowDateTime, anticipateTime)
            //println("Peek: The interval to the anticipate time $minutes")
            val timer = Timer()
            val task: TimerTask = object : TimerTask() {
                override fun run() {
                    nowDate = LocalDate.now()
                    println("Now date is $nowDate")
                    SaveDataToRoomDB()
                }
            }
            timer.schedule(task, 0,  1000 * 60 * 3)
        }

//        sync dexcom data every five minutes
        val dexcomTimer = Timer()
        val dexcomTimerTask = object: TimerTask() {
            override fun run() {
                if (DexcomUtil.readAuthState(applicationContext) != null) {
                    println("debug: sync in service")
                    try {
                        DexcomUtil.syncEgvs(applicationContext)
                    } catch (e: UnauthorizedException) {
                        println("debug: enter exception")
                        DexcomUtil.deleteAuthState(applicationContext)
                    }

                }
            }
        }
        dexcomTimer.schedule(dexcomTimerTask, 1000, 1000 * 60 * 5)

//        sleep api
        sleepPendingIntent =
            SleepReceiver.createSleepReceiverPendingIntent(context = applicationContext)
        subscribeToSleepSegmentUpdates(applicationContext, sleepPendingIntent)

    }

    private fun SaveActivityTimestamp(label: Int, timestamp: String){
        //val isExist = timestampRepository.checkExistanceOfTheDay("%" + LocalDate.now().toString() + "%")
        var type: String = ""
        when (label) {
            DetectedActivity.IN_VEHICLE -> {
                type = "Vehicle"
            }
            DetectedActivity.ON_BICYCLE -> {
                type = "Bicycle"
            }
            DetectedActivity.ON_FOOT -> {
                type = "OnFoot"
            }
            DetectedActivity.RUNNING -> {
                type = "Running"
            }
            DetectedActivity.STILL -> {
                type = "StandStill"
            }
            DetectedActivity.TILTING -> {
                type = "Tilting"
            }
            DetectedActivity.WALKING -> {
                type = "Walking"
            }
            DetectedActivity.UNKNOWN -> {
                type = "Unknown"
            }
        }


        val activityTimestampForOneDay = ActivityTimestampEntity()
        activityTimestampForOneDay.TimestampList = timestamp
        activityTimestampForOneDay.ActivityType = type
        println("Type of this time is $type")
        println("Timestamp is $timestamp")
        timestampRepository.insert(activityTimestampForOneDay)
        addActivityTimestampToFirebase(type, timestamp)

    }

    private fun addActivityTimestampToFirebase(
        activity_type: String,
        timestamp: String
    ) {
        if (activity_type.isNotEmpty()) {
            scope.launch {
                val convertedToEntityVersion = ArrayList<ActivityTimestampEntity>()
                val pushRef = FirebaseDatabaseUtil.generateId("activityTimestamp")
                //val activityTimestampEntity = ActivityTimestampEntity
                val activityTimestampSendEntity = ActivityTimestampEntity.from(pushRef!!.key!!, timestamp, activity_type)
                activityTimestampSendEntity.TimestampList = timestamp
                activityTimestampSendEntity.ActivityType = activity_type
                convertedToEntityVersion.add(activityTimestampSendEntity)
                activityTimestampSendEntity.id?.let { id ->
                                    FirebaseDatabaseUtil.saveWithId("activityTimestamp",
                                        id.toString(), activityTimestampSendEntity)}
                }
            }
        }

    fun addActivityRecognitionToFirebase(
        date: String,
        stillTime: Float,
        walkTime: Float,
        runTime: Float,
        vehicleTime: Float,
        bicycleTime: Float,
        onfootTime: Float,
        tiltTime: Float,
        unknownTime: Float
    ) {
        if (date.isNotEmpty()) {
            scope.launch {
                val convertedToEntityVersion = ArrayList<ActivityRecognitionEntity>()
                val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
                    FirebaseDatabaseUtil.firebaseDatabase.getReference(user.uid).child("activityRecognition")}
                println("ActivityRecognitionEntity::Date.name is :${ActivityRecognitionEntity::Date.name}")
                println("date is $date")
                myRef?.orderByChild("date")
                    ?.equalTo(date)
                    ?.get()
                    ?.addOnSuccessListener{
                        println("Debug: Try to create activityRecognition")
                        if (it.value == null){
                            val pushRef = FirebaseDatabaseUtil.generateId("activityRecognition")
                            println("Enter if :${it.value}")
                            //val activityTimestampEntity = ActivityTimestampEntity
                            val activityRecognitionSendEntity = ActivityRecognitionEntity.from(pushRef!!.key!!,
                                date, stillTime, walkTime, runTime, vehicleTime, bicycleTime, onfootTime, tiltTime, unknownTime)
                            activityRecognitionSendEntity.Date = date
                            activityRecognitionSendEntity.StillTime = stillTime
                            activityRecognitionSendEntity.WalkTime = walkTime
                            activityRecognitionSendEntity.RunTime = runTime
                            activityRecognitionSendEntity.VehicleTime = vehicleTime
                            activityRecognitionSendEntity.BicycleTime = bicycleTime
                            activityRecognitionSendEntity.OnFootTime = onfootTime
                            activityRecognitionSendEntity.TiltTime = tiltTime
                            activityRecognitionSendEntity.UnknownTime = unknownTime
                            convertedToEntityVersion.add(activityRecognitionSendEntity)
                            activityRecognitionSendEntity.sequenceId?.let { id ->
                                FirebaseDatabaseUtil.saveWithId("activityRecognition",
                                    id.toString(), activityRecognitionSendEntity)}
                        }
                        else {
                            myRef.orderByChild("date").equalTo(date)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        for (datasnap in snapshot.children) {
                                            datasnap.child("stillTime").ref.setValue(stillTime)
                                                .addOnSuccessListener {
                                                    println("Debug: Success change the value of stillTime")
                                                    datasnap.child("runTime").ref.setValue(runTime)
                                                    datasnap.child("walkTime").ref.setValue(walkTime)
                                                    datasnap.child("vehicleTime").ref.setValue(vehicleTime)
                                                    datasnap.child("bicycleTime").ref.setValue(bicycleTime)
                                                    datasnap.child("onFootTime").ref.setValue(onfootTime)
                                                    datasnap.child("tiltTime").ref.setValue(tiltTime)
                                                    datasnap.child("unknownTime").ref.setValue(unknownTime)
                                                }.addOnFailureListener{
                                                    println("Debug: fail to change the value of stillTime")
                                                }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        throw error.toException()
                                    }
                                })
//                            var query = myRef.orderByChild("date").equalTo(date).get()
//                            query.once("value", function(snapshot) {
//                                snapshot.ref.update({ displayName: "New trainer" })
//                            });
////                            val = myRef.orderByChild("date").equalTo(date).push().toString()
////                            println("Enter else: ${key}")
                        }

            }

            }
        }
    }

    //Data Saving function
    private fun SaveDataToRoomDB(){
        val isExist = repository.checkExistanceOfTheDay("%" + LocalDate.now().toString() + "%")
        if(isExist == 0) {
            val activityForOneDay = ActivityRecognitionEntity()
            activityForOneDay.Date = LocalDate.now().toString()
            activityForOneDay.StillTime = standStillTime
            activityForOneDay.BicycleTime = bicycleTime
            activityForOneDay.OnFootTime = onFootTime
            activityForOneDay.WalkTime = walkTime
            activityForOneDay.RunTime = runningTime
            activityForOneDay.VehicleTime = vehicleTime
            activityForOneDay.TiltTime = tiltTime
            activityForOneDay.UnknownTime = unknownTime
            repository.insert(activityForOneDay)//(activityForOneDay)

            addActivityRecognitionToFirebase(LocalDate.now().toString(),
                standStillTime,
                walkTime,
                runningTime,
                vehicleTime,
                bicycleTime,
                onFootTime,
                tiltTime,
                unknownTime)
        }
        else if(isExist != 0){
            val result  = repository.getSingleDayActivityDuration(LocalDate.now().toString())
            repository.updateStillDataOfToday(result.StillTime + standStillTime, LocalDate.now().toString())
            repository.updateWalkDataOfToday(result.WalkTime + walkTime, LocalDate.now().toString())
            repository.updateRunDataOfToday(result.RunTime + runningTime, LocalDate.now().toString())
            repository.updateVehicleDataOfToday(result.VehicleTime + vehicleTime, LocalDate.now().toString())
            repository.updateBicycleDataOfToday(result.BicycleTime + bicycleTime, LocalDate.now().toString())
            repository.updateOnFootDataOfToday(result.OnFootTime + onFootTime, LocalDate.now().toString())
            repository.updateTiltDataOfToday(result.TiltTime + tiltTime, LocalDate.now().toString())
            repository.updateUnknownDataOfToday(result.UnknownTime + unknownTime, LocalDate.now().toString())

            addActivityRecognitionToFirebase(LocalDate.now().toString(),
                result.StillTime + standStillTime,
                result.WalkTime + walkTime,
                result.RunTime + runningTime,
                result.VehicleTime + vehicleTime,
                result.BicycleTime + bicycleTime,
                result.OnFootTime + onFootTime,
                result.TiltTime + tiltTime,
                result.UnknownTime + unknownTime)
        }
        println("Debug: Auto save")


        standStillTime = 0F
        bicycleTime = 0F
        onFootTime = 0F
        walkTime = 0F
        runningTime = 0F
        vehicleTime = 0F
        tiltTime = 0F
        unknownTime = 0F
    }

    //Decode the type and confidence
    private fun handleUserActivity(type: Int, confidence: Int) {
 //       lateinit var templabel: Int
 //       startTimestamp = Calendar.getInstance()
        when (type) {
            DetectedActivity.IN_VEHICLE -> {
                println("Enter vehicle $confidence")
                if(confidence > 70) {
                    startTimestamp = Calendar.getInstance()
                    templabel = DetectedActivity.IN_VEHICLE
                    println("Observe: vehicle timestamp is ${LocalDateTime.now().toString()}")
                    if(templabel != tempType) {
                        SaveActivityTimestamp(templabel, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString())
                    }
                }
            }
            DetectedActivity.ON_BICYCLE -> {
                println("Enter bicycle $confidence")
                if(confidence > 70) {
                    startTimestamp = Calendar.getInstance()
                    templabel = DetectedActivity.ON_BICYCLE
                    println("Observe: bicycle timestamp is ${LocalDateTime.now().toString()}")
                    if(templabel != tempType) {
                        SaveActivityTimestamp(templabel, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString())
                    }
                }
            }
            DetectedActivity.ON_FOOT -> {
                println("Enter on Foot $confidence")
                if(confidence > 70) {
                    startTimestamp = Calendar.getInstance()
                    templabel = DetectedActivity.ON_FOOT
                    println("Observe: onfoot timestamp is ${LocalDateTime.now().toString()}")
                    if(templabel != tempType) {
                        SaveActivityTimestamp(templabel, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString())
                    }
                }
            }
            DetectedActivity.RUNNING -> {
                println("Enter running $confidence")
                if(confidence > 70) {
                    startTimestamp = Calendar.getInstance()
                    templabel = DetectedActivity.RUNNING
                    println("Observe: running timestamp is ${LocalDateTime.now().toString()}")
                    if(templabel != tempType) {
                        SaveActivityTimestamp(templabel, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString())
                    }
                }
            }
            DetectedActivity.STILL -> {
                println("Enter still $confidence")
                if(confidence > 70) {
                    startTimestamp = Calendar.getInstance()
                    templabel = DetectedActivity.STILL
                    println("Observe: standstill timestamp is ${LocalDateTime.now().toString()}")
                    if(templabel != tempType) {
                        SaveActivityTimestamp(templabel, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString())
                    }
                }
            }
            DetectedActivity.TILTING -> {
                println("Enter tilting $confidence")
                if(confidence > 70) {
                    startTimestamp = Calendar.getInstance()
                    templabel = DetectedActivity.TILTING
                    println("Observe: tilting timestamp is ${LocalDateTime.now().toString()}")
                    if(templabel != tempType) {
                        SaveActivityTimestamp(templabel, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString())
                    }
                }
            }
            DetectedActivity.WALKING -> {
                println("Enter walking $confidence")
                if(confidence > 70) {
                    startTimestamp = Calendar.getInstance()
                    templabel = DetectedActivity.WALKING
                    println("Observe: walking timestamp is ${LocalDateTime.now().toString()}")
                    if(templabel != tempType) {
                        SaveActivityTimestamp(templabel, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString())
                    }
                }
            }
            DetectedActivity.UNKNOWN -> {
                println("Enter unknown $confidence")
                if(confidence > 70) {
                    startTimestamp = Calendar.getInstance()
                    templabel = DetectedActivity.UNKNOWN
                    println("Observe: unknown timestamp is ${LocalDateTime.now().toString()}")
                    if(templabel != tempType) {
                        SaveActivityTimestamp(templabel, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString())
                    }
                }
            }
        }

        if(enterFlag != 0){
            var duration = startTimestamp.timeInMillis - tempTimestamp.timeInMillis
            when (tempType) {
                DetectedActivity.IN_VEHICLE -> {
                    vehicleTime += duration/1000
                }
                DetectedActivity.ON_BICYCLE -> {
                    bicycleTime += duration/1000
                }
                DetectedActivity.ON_FOOT -> {
                    onFootTime += duration/1000
                }
                DetectedActivity.RUNNING -> {
                    runningTime += duration/1000
                }
                DetectedActivity.STILL -> {
                    standStillTime += duration/1000
                }
                DetectedActivity.TILTING -> {
                    tiltTime += duration/1000
                }
                DetectedActivity.WALKING -> {
                    walkTime += duration/1000
                }
                DetectedActivity.UNKNOWN -> {
                    unknownTime += duration/1000
                }
            }
        }
        tempTimestamp = startTimestamp
        tempType = templabel
        enterFlag = 1

    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        msgHandler = null
        return true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY  //What does that mean?
    }

    fun requestActivityUpdatesButtonHandler() {
        val task = mActivityRecognitionClient?.requestActivityUpdates(
            DETECTION_INTERVAL_IN_MILLISECONDS,
            mPendingIntent)
        println("Debug: Is requestActivityUpdatesButtonHandler called?")
        task?.addOnSuccessListener {
            Toast.makeText(applicationContext,
                "Successfully requested activity updates",
                Toast.LENGTH_SHORT)
                .show()
        }
        task?.addOnFailureListener {
            Toast.makeText(applicationContext,
                "Requesting activity updates failed to start",
                Toast.LENGTH_SHORT)
                .show()
        }
    }
    fun removeActivityUpdatesButtonHandler() {
        val task = mActivityRecognitionClient?.removeActivityUpdates(
            mPendingIntent)
        task?.addOnSuccessListener {
            Toast.makeText(applicationContext,
                "Removed activity updates successfully!",
                Toast.LENGTH_SHORT)
                .show()
        }
        task?.addOnFailureListener {
            Toast.makeText(applicationContext, "Failed to remove activity updates!",
                Toast.LENGTH_SHORT).show()
        }
    }

    //Pass information thru messager
    fun PassActivityInfo() {
        try {
            counter += 1
            val tempHandler = msgHandler
            if (tempHandler != null) {  //when click the onbind button
                val bundle = Bundle() //Create the bundle to put information in
                bundle.putFloat(ACTIVITY_STILL_KEY, standStillTime)
                bundle.putFloat(ACTIVITY_WALK_KEY, walkTime)
                bundle.putFloat(ACTIVITY_RUN_KEY, runningTime)
                bundle.putFloat(ACTIVITY_FOOT_KEY, onFootTime)
                bundle.putFloat(ACTIVITY_VEHICLE_KEY, vehicleTime)
                bundle.putFloat(ACTIVITY_BICYCLE_KEY, bicycleTime)
                bundle.putFloat(ACTIVITY_TILT_KEY, tiltTime)
                bundle.putFloat(ACTIVITY_UNKNOWN_KEY, unknownTime)
                bundle.putInt(ACTIVITY_OBSERVER_KEY, counter)

                val message: Message = tempHandler.obtainMessage()
                message.data = bundle //Put the info in bundle into the message.data
                message.what = ACTIVITY_RECOGNITION_MSG
                tempHandler.sendMessage(message) //Send the message to the viewmodel via handler


            }
        } catch (t: Throwable) {
            println("Pass activity info failed. $t")
        }
    }

    private fun subscribeToSleepSegmentUpdates(context: Context, pendingIntent: PendingIntent) {
        println("debug: sleep requestSleepSegmentUpdates()")
        val task = ActivityRecognition.getClient(context).requestSleepSegmentUpdates(
            pendingIntent,
            // Registers for both [SleepSegmentEvent] and [SleepClassifyEvent] data.
            SleepSegmentRequest.getDefaultSleepSegmentRequest()
        )

        task.addOnSuccessListener {
//            mainViewModel.updateSubscribedToSleepData(true)
            println("debug: sleep Successfully subscribed to sleep data.")
        }
        task.addOnFailureListener { exception ->
            println("debug: sleep Exception when subscribing to sleep data: $exception")
        }
    }

    private fun unsubscribeToSleepSegmentUpdates(context: Context, pendingIntent: PendingIntent) {
        println("debug: sleep unsubscribeToSleepSegmentUpdates()")
        val task = ActivityRecognition.getClient(context).removeSleepSegmentUpdates(pendingIntent)

        task.addOnSuccessListener {
//            mainViewModel.updateSubscribedToSleepData(false)
            println("debug: sleep Successfully unsubscribed to sleep data.")
        }
        task.addOnFailureListener { exception ->
            println("debug: sleep Exception when unsubscribing to sleep data: $exception")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeActivityUpdatesButtonHandler()
        counter = 0
        standStillTime = 0F
        bicycleTime = 0F
        onFootTime = 0F
        walkTime = 0F
        runningTime = 0F
        vehicleTime = 0F
        tiltTime = 0F
        unknownTime = 0F
        unsubscribeToSleepSegmentUpdates(applicationContext, sleepPendingIntent)
    }
}