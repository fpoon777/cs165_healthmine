package com.example.healthmine.ui.sleep

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.healthmine.database.HealthmineDatabase
import com.example.healthmine.database.SleepRepository
import com.example.healthmine.models.SleepClassifyEventEntity
import com.example.healthmine.models.SleepSegmentEventDTO
import com.example.healthmine.models.SleepSegmentEventEntity
import com.example.healthmine.utils.FirebaseDatabaseUtil
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Saves Sleep Events to Database.
 */
class SleepReceiver : BroadcastReceiver() {

    // Used to launch coroutines (non-blocking way to insert data).
    private val scope: CoroutineScope = MainScope()



    override fun onReceive(context: Context, intent: Intent) {
        println("debug: sleep onReceive(): $intent")

        val database = HealthmineDatabase.getInstance(context)
        val sleepClassifyEventDao = database.sleepClassifyEventDao
        val sleepSegmentEventDao = database.sleepSegmentEventDao
        val repository = SleepRepository(sleepSegmentEventDao, sleepClassifyEventDao)
        
        if (SleepSegmentEvent.hasEvents(intent)) {
            val sleepSegmentEvents: List<SleepSegmentEvent> =
                SleepSegmentEvent.extractEvents(intent)
//            Log.d(TAG, "SleepSegmentEvent List: $sleepSegmentEvents")
            addSleepSegmentEventsToDatabase(repository, sleepSegmentEvents)
            println("debug: sleep sleepSegmentEvents ${sleepSegmentEvents.size}")
        } else if (SleepClassifyEvent.hasEvents(intent)) {
            val sleepClassifyEvents: List<SleepClassifyEvent> =
                SleepClassifyEvent.extractEvents(intent)
//            Log.d(TAG, "SleepClassifyEvent List: $sleepClassifyEvents")
            addSleepClassifyEventsToDatabase(repository, sleepClassifyEvents)
            println("debug: sleep sleepClassifyEvents ${sleepClassifyEvents.size}")
        }
    }

    private fun addSleepSegmentEventsToDatabase(
        repository: SleepRepository,
        sleepSegmentEvents: List<SleepSegmentEvent>
    ) {
        if (sleepSegmentEvents.isNotEmpty()) {
            scope.launch {
                val convertedToEntityVersion = ArrayList<SleepSegmentEventEntity>()
                sleepSegmentEvents.forEach { sleepSegmentEvent ->
                    val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
                        FirebaseDatabaseUtil.firebaseDatabase.getReference(user.uid).child("sleep")
                    }
                    myRef?.orderByChild(SleepSegmentEventEntity::startTime.name)
                        ?.equalTo(Instant.ofEpochMilli(sleepSegmentEvent.startTimeMillis)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        )?.get()
                        ?.addOnSuccessListener {
                        if (it.value == null) {
                            val pushRef = FirebaseDatabaseUtil.generateId("sleep")
                            val sleepSegmentEventEntity = SleepSegmentEventEntity.from(pushRef!!.key, sleepSegmentEvent)
                            convertedToEntityVersion.add(sleepSegmentEventEntity)
                            sleepSegmentEventEntity.id?.let { id ->
                                FirebaseDatabaseUtil.saveWithId("sleep",
                                    id, SleepSegmentEventDTO.from(sleepSegmentEventEntity))
                            }
                        }
                        println("debug: firebase " + "Got value ${it.value}")
                    }?.addOnFailureListener{
                        println("debug: firebase Error getting data $it")
                    }
                }

                repository.insertSleepSegments(convertedToEntityVersion)
            }
        }
    }

    private fun addSleepClassifyEventsToDatabase(
        repository: SleepRepository,
        sleepClassifyEvents: List<SleepClassifyEvent>
    ) {
        if (sleepClassifyEvents.isNotEmpty()) {
            scope.launch {
                val convertedToEntityVersion: List<SleepClassifyEventEntity> =
                    sleepClassifyEvents.map {
                        SleepClassifyEventEntity.from(it)
                    }
                repository.insertSleepClassifyEvents(convertedToEntityVersion)
            }
        }
    }

    companion object {
        const val TAG = "SleepReceiver"
        fun createSleepReceiverPendingIntent(context: Context): PendingIntent {
            val sleepIntent = Intent(context, SleepReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                0,
                sleepIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }
    }
}
