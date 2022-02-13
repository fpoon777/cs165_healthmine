package com.example.healthmine.ui.activityrecognition

import android.app.IntentService
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class DetectedActivitiesIntentService : IntentService(TAG) {
    override fun onCreate() {
        super.onCreate()
    }
    override fun onHandleIntent(intent: Intent?) {
        val result = ActivityRecognitionResult.extractResult(intent)
        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        val detectedActivities = result.probableActivities as ArrayList<*>
        for (activity in detectedActivities) {
            broadcastActivity(activity as DetectedActivity)
        }
        println("Debug: detectedActivities get")
    }
    private fun broadcastActivity(activity: DetectedActivity) {
        val intent = Intent(ActivityRecognition.BROADCAST_DETECTED_ACTIVITY)
        intent.putExtra("type", activity.type)
        intent.putExtra("confidence", activity.confidence)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        println("Debug: broadcastActivity is called ${activity.type}")
    }

    companion object {
        protected val TAG = DetectedActivitiesIntentService::class.java.simpleName
    }
}// Use the TAG to name the worker thread.