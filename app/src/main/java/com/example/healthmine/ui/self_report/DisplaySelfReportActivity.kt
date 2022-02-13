package com.example.healthmine.ui.self_report

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.healthmine.R
import com.example.healthmine.database.HealthmineDatabase
import com.example.healthmine.databinding.ActivityDisplaySelfReportBinding
import com.example.healthmine.ui.self_report.self_report_db.SelfReportDbDao
import com.example.healthmine.ui.self_report.self_report_db.SelfReportRepo

class DisplaySelfReportActivity: AppCompatActivity() {
    private lateinit var binding: ActivityDisplaySelfReportBinding

    private lateinit var textViewDateTime:TextView
    private lateinit var textViewRating:TextView
    private lateinit var textViewDiet:TextView
    private lateinit var textViewMedication:TextView
    private lateinit var textViewHealthInfo:TextView
    private lateinit var textViewLifeStyleInfo:TextView

    //create database
    private lateinit var database: HealthmineDatabase
    private lateinit var databaseDao: SelfReportDbDao
    private lateinit var repository: SelfReportRepo
    private lateinit var selfReportViewModel: SelfReportViewModel
    private lateinit var factoryModel:SelfReportViewFactory

    //delete selfreport
    private lateinit var deleteBtn:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras
        val entryId = bundle!!.getString("id", "-1")?.toLong()
        binding = ActivityDisplaySelfReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textViewDateTime = binding.entryDateTime
        textViewRating = binding.entryHealthRate
        textViewDiet = binding.entryChangeDiet
        textViewMedication = binding.entryChangeMedication
        textViewHealthInfo = binding.entryHealthInfo
        textViewLifeStyleInfo = binding.entryLifestyleInfo

        deleteBtn = binding.displayReportDeleteBtn

        //access database
        database = HealthmineDatabase.getInstance(this)
        databaseDao = database.selfReportDatabaseDao
        repository = SelfReportRepo(databaseDao)
        factoryModel= SelfReportViewFactory(repository)
        selfReportViewModel = ViewModelProvider(this, factoryModel)
            .get(SelfReportViewModel::class.java)
        selfReportViewModel.getItem(entryId!!)
        selfReportViewModel.oneEntryLiveData.observe(this){
            try{
            textViewDateTime.text = it.dateTime
            textViewRating.text = applicationContext.resources.getStringArray(R.array.self_report_spinner_items)[it.rateHealth]
            textViewDiet.text = it.changeDiet.toString()
            textViewMedication.text = it.changeMedication.toString()
            textViewHealthInfo.text =it.additionalInfo
            textViewLifeStyleInfo.text = it.otherChanges
            }
            catch (e:Exception){
                //do nothing
            }
        }

        deleteBtn.setOnClickListener {
            Toast.makeText(this, "DELETE clicked", Toast.LENGTH_SHORT).show()
            if (entryId != -1L){
                selfReportViewModel.deleteThisEntry(entryId!!)
            }
            finish()
        }
    }
}