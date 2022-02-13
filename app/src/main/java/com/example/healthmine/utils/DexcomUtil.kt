package com.example.healthmine.utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.healthmine.database.EgvRepository
import com.example.healthmine.database.HealthmineDatabase
import com.example.healthmine.models.AverageEgv
import com.example.healthmine.models.EGV
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDateTime
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.format.DateTimeFormatter


object DexcomUtil {
    fun readAuthState(context: Context): AuthState? {
        val authPrefs = context.getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
        val stateJson = authPrefs.getString("stateJson", null)
        return if (stateJson != null) {
            AuthState.jsonDeserialize(stateJson)
        } else {
            null
        }
    }

    fun writeAuthState(context: Context, state: AuthState) {
        val authPrefs = context.getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
        authPrefs.edit()
            .putString("stateJson", state.jsonSerializeString())
            .apply()
    }

    fun deleteAuthState(context: Context) {
        val authPrefs = context.getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
        authPrefs.edit()
            .remove("stateJson")
            .apply()
    }

//    get egvs date range, results will be returned in a LocalDateTime list.
//    The first item is start system time, the second item is start display time,
//    the third item is end system time and the fourth item is end display time.
//    If no auth state, an UnauthorizedException will be thrown.
    suspend fun getEgvsDateRange(context: Context): List<LocalDateTime> {
        val localDateTimeList = ArrayList<LocalDateTime>()
        coroutineScope {
            val authState = readAuthState(context) ?: throw UnauthorizedException("No Authorization")
            authState.performActionWithFreshTokens(AuthorizationService(context),
                AuthState.AuthStateAction { accessToken, idToken, ex ->
                    if (ex != null) {
                        // negotiation for fresh tokens failed, check ex for more details
                        throw UnauthorizedException(ex.toString())
                    }

//                    update auth state
                    writeAuthState(context, authState)

                    // use the access token to do something ...
                    println("debug: $accessToken and $idToken")

                    val client = OkHttpClient()

                    val request: Request = Request.Builder()
                        .url("https://sandbox-api.dexcom.com/v2/users/self/dataRange")
                        .get()
                        .addHeader("authorization", "Bearer $accessToken")
                        .build()

                    val response = client.newCall(request).execute()
                    println("debug: ${response.code}")
                    val json = JSONObject(response.body?.string()!!)
                    val startSystemTime = LocalDateTime.parse(
                        json.getJSONObject("egvs")
                            .getJSONObject("start")
                            .getString("systemTime")
                    )
                    val startDisplayTime = LocalDateTime.parse(
                        json.getJSONObject("egvs")
                            .getJSONObject("start")
                            .getString("displayTime")
                    )
                    val endSystemTime = LocalDateTime.parse(
                        json.getJSONObject("egvs")
                            .getJSONObject("end")
                            .getString("systemTime")
                    )
                    val endDisplayTime = LocalDateTime.parse(
                        json.getJSONObject("egvs")
                            .getJSONObject("end")
                            .getString("displayTime")
                    )
                    localDateTimeList.add(startSystemTime)
                    localDateTimeList.add(startDisplayTime)
                    localDateTimeList.add(endSystemTime)
                    localDateTimeList.add(endDisplayTime)
                })
        }
        return localDateTimeList
    }

//    Return list of egv
//    If no auth state, an UnauthorizedException will be thrown.
    suspend fun getEgvs(context: Context, startSystemTime: LocalDateTime, endSystemTime: LocalDateTime): List<EGV> {
        val egvs = ArrayList<EGV>()
        coroutineScope {
            val authState = readAuthState(context) ?: throw UnauthorizedException("No Authorization")
            authState.performActionWithFreshTokens(AuthorizationService(context),
                AuthState.AuthStateAction { accessToken, idToken, ex ->
                    if (ex != null) {
                        // negotiation for fresh tokens failed, check ex for more details
                        throw UnauthorizedException(ex.toString())
                    }

//                  update auth state
                    writeAuthState(context, authState)

                    // use the access token to do something ...
                    println("debug: $accessToken and $idToken")

                    val client = OkHttpClient()

                    val httpBuilder = "https://sandbox-api.dexcom.com/v2/users/self/egvs".toHttpUrlOrNull()!!.newBuilder()
                    httpBuilder
                        .addQueryParameter("startDate", startSystemTime.format(
                        DateTimeFormatter.ISO_DATE_TIME))
                        .addQueryParameter("endDate", endSystemTime.format(DateTimeFormatter.ISO_DATE_TIME))
                    val request: Request = Request.Builder()
                        .url(httpBuilder.build())
                        .get()
                        .addHeader("authorization", "Bearer $accessToken")
                        .build()

                    val response = client.newCall(request).execute()
                    val json = JSONObject(response.body?.string()!!)
//                    println("debug: length ${response.code}")
//                    println("debug: length ${json.getJSONArray("errors")}")
                    val objectMapper = ObjectMapper()
                    objectMapper.findAndRegisterModules()
                    egvs.addAll(objectMapper.readValue(json.getJSONArray("egvs").toString(), object : TypeReference<List<EGV>>() { }))
                    println("debug: ${json.getJSONArray("egvs").toString()}")
                })
        }
        return egvs.reversed()
    }

    suspend fun getUnlimitedEgvs(context: Context,
                                 startSystemTime: LocalDateTime,
                                 endSystemTime: LocalDateTime): List<EGV> {
        val egvs = ArrayList<EGV>()
        val dateRangeLimit = startSystemTime.plusDays(90)
        if (endSystemTime.isAfter(dateRangeLimit)) {
            egvs.addAll(getEgvs(context, startSystemTime, dateRangeLimit))
            egvs.addAll(getUnlimitedEgvs(context,
                dateRangeLimit.plusSeconds(1), endSystemTime))
        } else {
            egvs.addAll(getEgvs(context, startSystemTime, endSystemTime))
        }
        return egvs
    }

    fun saveHourlyEgvs(context: Context, egvs: List<EGV>) {
//        get latest, if null save all, if not null, calcualte between
        val database = HealthmineDatabase.getInstance(context)
        val databaseDao = database.egvDao
        val repository = EgvRepository(databaseDao)

        var averageEgvs = ArrayList<AverageEgv>()

        val latestAvgEgv: AverageEgv?

        runBlocking {
            latestAvgEgv = repository.getLatestAverageEgvByTag(0)
        }

//        val latestEgv = repository.getLatestEgv()
        val earliestEgv = egvs.first()
        var startTime = if (latestAvgEgv == null) {
            earliestEgv.systemTime!!
                .withMinute(0).withSecond(0).withNano(0)
        } else {
            latestAvgEgv.systemTime!!
        }
        var endTime = startTime.plusHours(1)

        var currentEgvs = ArrayList<EGV>()
        egvs.forEach {
            if (!it.systemTime!!.isBefore(endTime)) {
                val averageEgv = getEgvMean(currentEgvs, startTime, 0)
                currentEgvs = ArrayList()
                averageEgvs.add(averageEgv)
                startTime = endTime
                endTime = startTime.plusHours(1)
            }
            if (it.systemTime!!.isAfter(startTime)) {
                currentEgvs.add(it)
            }
        }

        if (!averageEgvs.isNullOrEmpty()) {
            if (latestAvgEgv != null) {
                val latestId = latestAvgEgv.id

                val newAverageEgv = averageEgvs[0]
                newAverageEgv.id = latestId
                repository.updateAverageEgv(newAverageEgv)
                averageEgvs.removeAt(0)
            }
//            repository.deleteAverageEgvById()
            runBlocking {
                repository.insertAllAverageEgvs(averageEgvs)
            }
        }
    }

    fun saveDailyEgvs(context: Context) {
//        get latest, if null save all, if not null, calcualte between
        val database = HealthmineDatabase.getInstance(context)
        val databaseDao = database.egvDao
        val repository = EgvRepository(databaseDao)

        var averageEgvs = ArrayList<AverageEgv>()

        val latestDailyEgv: AverageEgv?
        val latestHourlyEgv: AverageEgv?

        runBlocking {
            val latestDailyEgvDeferred = async { repository.getLatestAverageEgvByTag(1) }
            val latestHourlyEgvDeferred = async { repository.getLatestAverageEgvByTag(0) }
            latestDailyEgv = latestDailyEgvDeferred.await()
            latestHourlyEgv = latestHourlyEgvDeferred.await()
        }

//        val latestEgv = repository.getLatestEgv()
//        val earliestEgv = egvs.first()

        var startTime: LocalDateTime

        if (latestDailyEgv == null) {
            val earliestHourlyEgv: AverageEgv?
            runBlocking {
                earliestHourlyEgv = repository.getEarliestAverageEvgByTag(0)
            }
            if (earliestHourlyEgv == null) {
                return                                               
            } else {
                startTime = earliestHourlyEgv.systemTime!!.withHour(0)
            }
        } else {
            startTime = latestDailyEgv.systemTime!!
        }

        var endTime = startTime.plusDays(1)

        var currentEgvs = ArrayList<AverageEgv>()
        while (latestHourlyEgv?.systemTime!!.isAfter(startTime)) {
            runBlocking {
                currentEgvs.addAll(repository.getAvgEgvsBetween(0, startTime, endTime))
            }
            val averageEgv = getAvgEgvMean(currentEgvs, startTime, 1)
            averageEgvs.add(averageEgv)
            currentEgvs = ArrayList()
            startTime = endTime
            endTime = startTime.plusDays(1)
        }

        if (!averageEgvs.isNullOrEmpty()) {
            if (latestDailyEgv != null) {
                val latestId = latestDailyEgv.id

                val newAverageEgv = averageEgvs[0]
                newAverageEgv.id = latestId
                repository.updateAverageEgv(newAverageEgv)
                averageEgvs.removeAt(0)
            }
//            repository.deleteAverageEgvById()
            runBlocking {
                repository.insertAllAverageEgvs(averageEgvs)
            }
        }
    }

    fun saveMonthlyEgvs(context: Context) {
//        get latest, if null save all, if not null, calcualte between
        val database = HealthmineDatabase.getInstance(context)
        val databaseDao = database.egvDao
        val repository = EgvRepository(databaseDao)

        var averageEgvs = ArrayList<AverageEgv>()

        val latestMonthlyEgv: AverageEgv?
        val latestDailyEgv: AverageEgv?

        runBlocking {
            val latestMonthlyEgvDeferred = async { repository.getLatestAverageEgvByTag(2) }
            val latestDailyEgvDeferred = async { repository.getLatestAverageEgvByTag(1) }
            latestMonthlyEgv = latestMonthlyEgvDeferred.await()
            latestDailyEgv = latestDailyEgvDeferred.await()
        }

//        val latestEgv = repository.getLatestEgv()
//        val earliestEgv = egvs.first()

        var startTime: LocalDateTime

        if (latestMonthlyEgv == null) {
            val earliestDailyEgv: AverageEgv?
            runBlocking {
                earliestDailyEgv = repository.getEarliestAverageEvgByTag(1)
            }
            if (earliestDailyEgv == null) {
                return
            } else {
                startTime = earliestDailyEgv.systemTime!!.withDayOfMonth(1)
            }
        } else {
            startTime = latestMonthlyEgv.systemTime!!
        }

        var endTime = startTime.plusMonths(1)

        var currentEgvs = ArrayList<AverageEgv>()
        while (latestDailyEgv?.systemTime!!.isAfter(startTime)) {
            runBlocking {
                currentEgvs.addAll(repository.getAvgEgvsBetween(1, startTime, endTime))
            }
            val averageEgv = getAvgEgvMean(currentEgvs, startTime, 2)
            averageEgvs.add(averageEgv)
            currentEgvs = ArrayList()
            startTime = endTime
            endTime = startTime.plusMonths(1)
        }

        if (!averageEgvs.isNullOrEmpty()) {
            if (latestMonthlyEgv != null) {
                val latestId = latestMonthlyEgv.id

                val newAverageEgv = averageEgvs[0]
                newAverageEgv.id = latestId
                repository.updateAverageEgv(newAverageEgv)
                averageEgvs.removeAt(0)
            }
//            repository.deleteAverageEgvById()
            runBlocking {
                repository.insertAllAverageEgvs(averageEgvs)
            }
        }
    }

    fun getEgvMean(egvs: List<EGV>, time: LocalDateTime, tag: Int): AverageEgv {
        var sum = 0
        egvs.forEach {
            sum += it.value!!
        }
        val averageEgv = AverageEgv()
        averageEgv.value = sum / egvs.size
        averageEgv.systemTime = time
        averageEgv.tag = tag
        return averageEgv
    }

    fun getAvgEgvMean(avgEgvs: List<AverageEgv>, time: LocalDateTime, tag: Int): AverageEgv {
        var sum = 0
        avgEgvs.forEach {
            sum += it.value!!
        }
        val averageEgv = AverageEgv()
        averageEgv.value = sum / avgEgvs.size
        averageEgv.systemTime = time
        averageEgv.tag = tag
        return averageEgv
    }

    fun syncEgvs(context: Context) {
        val database = HealthmineDatabase.getInstance(context)
        val databaseDao = database.egvDao
        val repository = EgvRepository(databaseDao)

//        val latestEgv = repository.getLatestEgv()
        val latestHourlyEgv: AverageEgv?
        val dateTimeList: List<LocalDateTime>
        runBlocking {
            val dateTimeListDeferred = async { getEgvsDateRange(context) }
            val latestHourlyEgvDeferred = async { repository.getLatestAverageEgvByTag(0) }
            dateTimeList = dateTimeListDeferred.await()
            latestHourlyEgv = latestHourlyEgvDeferred.await()
        }

        if (dateTimeList.isNullOrEmpty()) {
            throw UnauthorizedException("No date range")
        }

        val startTime = if (latestHourlyEgv == null) {
            dateTimeList[0]
        } else {
            latestHourlyEgv.systemTime!!
        }
        println("debug: starttime $startTime")
        val endTime = dateTimeList[2]

        val egvList: List<EGV>

        runBlocking {
            egvList = getUnlimitedEgvs(context, startTime, endTime)
        }


//        repository.insertAll(egvList)
        saveHourlyEgvs(context, egvList)
        saveDailyEgvs(context)
        saveMonthlyEgvs(context)
    }
}