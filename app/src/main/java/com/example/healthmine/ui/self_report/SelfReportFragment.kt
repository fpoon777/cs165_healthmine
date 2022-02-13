package com.example.healthmine.ui.self_report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.healthmine.database.HealthmineDatabase
import com.example.healthmine.databinding.FragmentSelfReportBinding
import com.example.healthmine.ui.self_report.self_report_db.SelfReportDbDao
import com.example.healthmine.ui.self_report.self_report_db.SelfReportEntry
import com.example.healthmine.ui.self_report.self_report_db.SelfReportRepo
import java.util.*

// a fragment that holds the self report activity
class SelfReportFragment : Fragment() {
    private val tabTitles = arrayOf("Self Report", "History")
    private val MONTH_NAMES = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    private var _binding: FragmentSelfReportBinding? = null
    private val binding get() = _binding!!

    //save and cancel button
    private lateinit var saveBtn: Button
    private lateinit var cancelBtn: Button

    //variables to hold user inputs:
    private lateinit var healthSpinner: Spinner
    private lateinit var dietRadioGroup: RadioGroup
    private lateinit var medicationRadioGroup: RadioGroup
    private var healthInfo:String=""
    private var lifestyleInfo:String=""
    private lateinit var healthInfoEditText: EditText
    private lateinit var lifestyleInfoEditText: EditText

    private var firstBtnChoice:Int=0
    private var secondBtnChoice:Int=0

    private val calendar:Calendar = Calendar.getInstance()

    // datebase
    //create database
    private lateinit var database: HealthmineDatabase
    private lateinit var databaseDao: SelfReportDbDao
    private lateinit var repository: SelfReportRepo
    private lateinit var selfReportViewModel: SelfReportViewModel
    private lateinit var factoryModel:SelfReportViewFactory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSelfReportBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //initialization
        saveBtn = binding.selfReportSaveBtn
        cancelBtn = binding.selfReportCancelBtn
        healthSpinner = binding.healthSpinner
        dietRadioGroup = binding.dietGroup
        medicationRadioGroup = binding.medicationGroup
        healthInfoEditText = binding.selfReportInfoTextInput
        lifestyleInfoEditText = binding.selfReportLifestyleTextInput

        //access database
        database = HealthmineDatabase.getInstance(requireContext())
        databaseDao = database.selfReportDatabaseDao
        repository = SelfReportRepo(databaseDao)
        factoryModel= SelfReportViewFactory(repository)
        selfReportViewModel = ViewModelProvider(this, factoryModel)
            .get(SelfReportViewModel::class.java)

        //saved instance
        if(savedInstanceState != null){
            firstBtnChoice = savedInstanceState.getInt("firstBtnChoice")
            secondBtnChoice = savedInstanceState.getInt("secondBtnChoice")
            healthSpinner.setSelection(savedInstanceState.getInt("spinnerSelectPos"))
        }

        saveBtn.setOnClickListener {
            //do something here
            var entry = createEntry()
            selfReportViewModel.insert(entry)
            Toast.makeText(requireContext(), "SELF-REPORT SAVED!", Toast.LENGTH_SHORT).show()
            clearAll()      //clear all previous selections and inputs
        }

        cancelBtn.setOnClickListener {
            //do something here
            clearAll()
        }

        dietRadioGroup.setOnCheckedChangeListener { _, i ->
            onDietBtnClick()
        }

        medicationRadioGroup.setOnCheckedChangeListener { _, i ->
            onMedicationBtnClick()
        }

        return root
    }

    //Creates an entry for the database
    private fun createEntry(): SelfReportEntry{
        var dietChanged = false     //default value is false
        if (firstBtnChoice ==1){
            dietChanged = true
        }
        // format entries properly
        var healthSpinnerPos = healthSpinner.selectedItemPosition

        var dateString = "${MONTH_NAMES[calendar.get(Calendar.MONTH)]}. " +
                "${calendar.get(Calendar.DAY_OF_MONTH)} " + "${calendar.get(Calendar.YEAR)}"

        var timeString = "${calendar.get(Calendar.HOUR_OF_DAY)}:" +
                "${calendar.get(Calendar.MINUTE)}:" + "${calendar.get(Calendar.SECOND)}"
        var dateInput = "$dateString $timeString"
        var additionalInfoText = if (healthInfoEditText.text.toString() == "") "" else healthInfoEditText.text.toString()

        var medicationChanged = false
        if (secondBtnChoice ==1){
            medicationChanged = true
        }
        var lifeStyleInfoText = if (lifestyleInfoEditText.text.toString() == "") "" else lifestyleInfoEditText.text.toString()

        //create and modify entries
        var reportEntry = SelfReportEntry()
        reportEntry.changeDiet = dietChanged
        reportEntry.rateHealth = healthSpinnerPos
        reportEntry.dateTime = dateInput
        reportEntry.additionalInfo = additionalInfoText
        reportEntry.changeMedication = medicationChanged
        reportEntry.otherChanges = lifeStyleInfoText
        return reportEntry
    }

    // function that clears the inputs and selections
    private fun clearAll(){
        dietRadioGroup.clearCheck()
        medicationRadioGroup.clearCheck()
        healthInfoEditText.text.clear()
        lifestyleInfoEditText.text.clear()
        healthSpinner.setSelection(0)
    }

    private fun onDietBtnClick(){
        var radioId = dietRadioGroup.checkedRadioButtonId
        var btn = binding.settingFirstYesBtn.id
        if (radioId == btn){
            firstBtnChoice = 1      //when yes is clicked
        }
        else{
            firstBtnChoice = 2
        }
    }

    private fun onMedicationBtnClick(){
        var radioId = medicationRadioGroup.checkedRadioButtonId
        var btn = binding.settingSecondYesBtn.id
        if (radioId == btn){
            secondBtnChoice = 1      //when yes is clicked
        }
        else{
            secondBtnChoice = 2
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("firstBtnChoice", firstBtnChoice)
        outState.putInt("secondBtnChoice", secondBtnChoice)
        outState.putInt("spinnerSelectPos", healthSpinner.selectedItemPosition)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}