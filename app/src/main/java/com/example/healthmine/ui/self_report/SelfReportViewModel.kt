package com.example.healthmine.ui.self_report

import androidx.lifecycle.*
import com.example.healthmine.ui.self_report.self_report_db.SelfReportEntry
import com.example.healthmine.ui.self_report.self_report_db.SelfReportRepo
import kotlinx.coroutines.flow.Flow

class SelfReportViewModel(private val repository: SelfReportRepo):ViewModel() {
    val allExerciseEntriesLiveData = repository.allReportEntries.asLiveData()
    var oneEntryLiveData: LiveData<SelfReportEntry>

    init {
        oneEntryLiveData = MutableLiveData<SelfReportEntry>()
    }

    fun insert(entry: SelfReportEntry){
        repository.insert(entry)
    }

    //delete the entry in this activity page
    fun deleteThisEntry(id:Long){
        repository.delete(id)
    }

    //get a specific item by id
    fun getItem(id:Long){
        oneEntryLiveData = repository.getItemById(id).asLiveData()
//        return repository.getItemById(id).asLiveData()
    }

}

// factory class that takes in the repository
class SelfReportViewFactory(private val repository: SelfReportRepo):ViewModelProvider.Factory{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelfReportViewModel::class.java)){
            return SelfReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}