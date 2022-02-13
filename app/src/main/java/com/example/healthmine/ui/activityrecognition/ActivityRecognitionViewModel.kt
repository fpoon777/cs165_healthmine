package com.example.healthmine.ui.activityrecognition

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class ActivityRecognitionViewModel : ViewModel() , ServiceConnection {
    private lateinit var ActivityMessageHandler: ActRcoMessageHandler
    init {  //Don;t forget to initalized it
        ActivityMessageHandler = ActRcoMessageHandler(Looper.getMainLooper())
    }

    private var _stillActivity: Float = 0F //inside, can be updated
    val stillActivity: Float //outside, cannot be updated
        get() = _stillActivity
    private var _walkActivity: Float = 0F //inside, can be updated
    val walkActivity:  Float //outside, cannot be updated
        get() = _walkActivity
    private var _runActivity: Float = 0F //inside, can be updated
    val runActivity:  Float //outside, cannot be updated
        get() = _runActivity
    private var _vehicleActivity: Float = 0F//inside, can be updated
    val vehicleActivity:  Float //outside, cannot be updated
        get() = _vehicleActivity
    private var _onFootActivity :Float = 0F//inside, can be updated
    val onFootActivity: Float //outside, cannot be updated
        get() = _onFootActivity
    private var _tiltActivity : Float = 0F //inside, can be updated
    val tiltActivity:  Float //outside, cannot be updated
        get() = _tiltActivity
    private var _bicycleActivity : Float = 0F //inside, can be updated
    val bicycleActivity:  Float //outside, cannot be updated
        get() = _bicycleActivity
    private var _unknownActivity :Float = 0F//inside, can be updated
    val unknownActivity: Float //outside, cannot be updated
        get() = _unknownActivity
    private val _observeActivity = MutableLiveData<Int>() //inside, can be updated
    val observeActivity: LiveData<Int> //outside, cannot be updated
        get() = _observeActivity


    override fun onServiceConnected(name: ComponentName, iBinder: IBinder) {
        println("debug: ViewModel: onServiceConnected() called; ComponentName: $name")  //Service started
        val tempBinder = iBinder as BackgroundDetectedActivitiesService.LocalBinder
        tempBinder.setmsgHandler(ActivityMessageHandler)  //Start to get info from the service thru handler
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        println("debug: Activity: onServiceDisconnected() called~~~")
    }

    inner class ActRcoMessageHandler(looper: Looper) : Handler(looper) {  //Not use the default handler anymore
        override fun handleMessage(msg: Message) {
            if (msg.what == BackgroundDetectedActivitiesService.ACTIVITY_RECOGNITION_MSG) {  //filter something you dont' want
                val bundle = msg.data
                _stillActivity = bundle.getFloat(BackgroundDetectedActivitiesService.ACTIVITY_STILL_KEY)
                _walkActivity = bundle.getFloat(BackgroundDetectedActivitiesService.ACTIVITY_WALK_KEY)
                _runActivity = bundle.getFloat(BackgroundDetectedActivitiesService.ACTIVITY_RUN_KEY)
                _vehicleActivity = bundle.getFloat(BackgroundDetectedActivitiesService.ACTIVITY_VEHICLE_KEY)
                _bicycleActivity = bundle.getFloat(BackgroundDetectedActivitiesService.ACTIVITY_BICYCLE_KEY)
                _tiltActivity = bundle.getFloat(BackgroundDetectedActivitiesService.ACTIVITY_TILT_KEY)
                _onFootActivity  = bundle.getFloat(BackgroundDetectedActivitiesService.ACTIVITY_FOOT_KEY)
                _unknownActivity= bundle.getFloat(BackgroundDetectedActivitiesService.ACTIVITY_UNKNOWN_KEY)
                _observeActivity.value = bundle.getInt(BackgroundDetectedActivitiesService.ACTIVITY_OBSERVER_KEY)
                println("Viewmodel handler is still working")
                }
            }
        }
    }


