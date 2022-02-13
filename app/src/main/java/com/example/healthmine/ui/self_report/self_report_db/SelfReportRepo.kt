package com.example.healthmine.ui.self_report.self_report_db

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SelfReportRepo(private val selfReportDbDao: SelfReportDbDao) {
    val allReportEntries = selfReportDbDao.getAllExerciseEntries()

    fun insert(entry:SelfReportEntry){
        CoroutineScope(IO).launch {
            selfReportDbDao.insertExerciseEntry(entry)
        }
    }

    fun delete(id:Long){
        CoroutineScope(IO).launch {
            selfReportDbDao.deleteExerciseEntry(id)
        }
    }

    fun deleteAll(){
        CoroutineScope(IO).launch {
            selfReportDbDao.deleteAll()
        }
    }

    fun getItemById(id:Long): Flow<SelfReportEntry> = selfReportDbDao.getItemById(id)
}