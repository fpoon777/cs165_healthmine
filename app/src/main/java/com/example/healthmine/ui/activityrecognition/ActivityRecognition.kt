package com.example.healthmine.ui.activityrecognition

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthmine.R
import com.example.healthmine.database.*
import com.example.healthmine.databinding.ActivityRecognitionLayoutBinding
import com.example.healthmine.ui.cgm.CustomMonthPicker
import com.example.healthmine.ui.cgm.CustomYearPicker
import com.google.android.material.chip.Chip
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData

import com.github.mikephil.charting.data.PieDataSet

import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ActivityRecognition : Fragment() {

    private lateinit var activityRecognitionViewModel: ActivityRecognitionViewModel
    private var _binding: ActivityRecognitionLayoutBinding? = null
    private val PERMISSION_REQUEST_CODE = 0
    private val summaryList = ArrayList<SummaryModel>() //Create a kind of list type
//    private val historySummary = ArrayList<SummaryModel>()
    private lateinit var activityAdapter: ActivityAdapter
    private lateinit var historyAdapter: ActivityAdapter

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var day_chip: Chip
    private lateinit var week_chip: Chip
    private lateinit var month_chip: Chip
    private lateinit var year_chip: Chip
    var pieChart: PieChart? = null

    private lateinit var date_calendar: TextView
    private lateinit var selectedDate : Date
    private lateinit var selectedWeekStartDate: Date
    private lateinit var selectedWeekEndDate: Date
    private lateinit var selectedMonthAndYear: Date
    private lateinit var selectedYear: Date
    private lateinit var earliestC: Calendar
    private lateinit var recyclerView: RecyclerView

    private lateinit var datePicker: MaterialDatePicker<Long>
    private lateinit var datePickerForWeek: MaterialDatePicker<Long>
    private lateinit var customMonthPicker: CustomMonthPicker
    private lateinit var customYearPicker: CustomYearPicker

    private lateinit var stillSummary: SummaryModel
    private lateinit var walkSummary: SummaryModel
    private lateinit var runSummary: SummaryModel
    private lateinit var bicycleSummary: SummaryModel
    private lateinit var vehicleSummary: SummaryModel
    private lateinit var tiltSummary: SummaryModel
    private lateinit var onFootSummary: SummaryModel
    private lateinit var unknownSummary: SummaryModel

    private var stillEnterLabel: Int = 0
    private var still_index: Int = 0
    private var walkEnterLabel: Int = 0
    private var walk_index: Int = 0
    private var runEnterLabel: Int = 0
    private var run_index: Int = 0
    private var vehicleEnterLabel: Int = 0
    private var vehicle_index: Int = 0
    private var onFootEnterLabel: Int = 0
    private var onFoot_index: Int = 0
    private var bicycleEnterLabel: Int = 0
    private var bicycle_index: Int = 0
    private var unknownEnterLabel: Int = 0
    private var unknown_index: Int = 0
    private var tiltEnterLabel: Int = 0
    private var tilt_index: Int = 0

    private lateinit var activityRecognitionContext: Context
    private lateinit var activityViewModel: ActivityRecognitionViewModel

    lateinit var activityRecognitionDatabase: HealthmineDatabase
    lateinit var activityRecognitionDatabaseDao: ActivityRecognitionDao
    lateinit var repository: ActivityRecognitionRepository
    lateinit var viewModelFactory: ManualInputDataViewModelFactory
    lateinit var activityRecognitionDataViewModel: ActivityRecognitionDatabaseViewModel

    // DAY/WEEK/MONTH/YEAR chips groups
    private lateinit var activityChipGroup: ChipGroup
    // current checked chip
    private lateinit var checkedChip: Chip
    // currently checked chip's text
    private lateinit var checkedChipText: String
    //private lateinit var dateTextView: TextView
    private val monthName = arrayListOf<String>("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private lateinit var aDayBeforeDateImageView : ImageView
    private lateinit var aDayAfterDateImageView : ImageView
    private var enter_index: Int = 0

    private val activityServiceObserver = Observer<Int> { observeInstantActivity(it) }


    @SuppressLint("SetTextI18n", "NotifyDataSetChanged", "CutPasteId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //Check sensor permission
        checkPermission()

        activityRecognitionViewModel =
            ViewModelProvider(this).get(ActivityRecognitionViewModel::class.java)
        _binding = ActivityRecognitionLayoutBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Set the date calendar
        date_calendar = root.findViewById(R.id.show_the_date)

        //Define the chips
        activityChipGroup = root.findViewById<ChipGroup>(R.id.chipGroup)
        checkedChip = activityChipGroup.findViewById(R.id.day_chip)
        day_chip = activityChipGroup.findViewById(R.id.day_chip)
        week_chip = activityChipGroup.findViewById(R.id.week_chip)
        month_chip = activityChipGroup.findViewById(R.id.month_chip)
        year_chip = activityChipGroup.findViewById(R.id.year_chip)

        aDayBeforeDateImageView = root.findViewById<ImageView>(R.id.left_arrow)
        aDayAfterDateImageView = root.findViewById<ImageView>(R.id.right_arrow)


        date_calendar.text = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")).toString()

        //Deal with the activity summary
        //Define the database and related things:
        recyclerView = root.findViewById<RecyclerView>(R.id.recyclerView)
        activityAdapter = ActivityAdapter(summaryList)
        val mLayoutManager = LinearLayoutManager(activity?.applicationContext)
        mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = activityAdapter


        //Define the pie chart view
        pieChart = root.findViewById(R.id.pieChart)
        initPieChart() //Initialize pie chart

        //Define and initialize the map list
        //Need to receive data from view model
//        val intent = Intent(requireActivity(), BackgroundDetectedActivitiesService::class.java)
//        requireActivity().startService(intent)
//        activityRecognitionContext = requireActivity()//this.applicationContext
//        activityViewModel = ViewModelProvider(this).get(ActivityRecognitionViewModel::class.java)
//        activityRecognitionContext.bindService(intent, activityViewModel, Context.BIND_AUTO_CREATE)
//        startViewModelAndService()

        //When enter the UI, show the data from the current day(update "instantly")
        //initializeUIByDayData()


        activityRecognitionDatabase = HealthmineDatabase.getInstance(requireActivity())  //Create the database instance
        activityRecognitionDatabaseDao = activityRecognitionDatabase.activityDatabaseDao //Connect the dao with the database
        repository = ActivityRecognitionRepository(activityRecognitionDatabaseDao) //Connect the repository with the dao
        viewModelFactory = ManualInputDataViewModelFactory(repository) //Connect the viewmodel with the repository
        //Display the viewmodel through ViewModelProvider
        activityRecognitionDataViewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(ActivityRecognitionDatabaseViewModel::class.java)

        selectedDate = Date()

        earliestC = Calendar.getInstance()
        earliestC.set(Calendar.YEAR, 2018)
        earliestC.set(Calendar.MONTH, 0)
        earliestC.set(Calendar.DATE, 1)
        earliestC.set(Calendar.HOUR_OF_DAY, 0)
        earliestC.set(Calendar.MINUTE, 0)
        earliestC.set(Calendar.SECOND, 0)
        earliestC.set(Calendar.MILLISECOND, 0)

        // week
        val c = Calendar.getInstance()
        c.time = selectedDate
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.DATE, -6)
        selectedWeekStartDate = c.time

        //month
        c.time = selectedDate
        c.set(Calendar.DATE, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        selectedMonthAndYear = c.time

        // year
        c.set(Calendar.MONTH, 0)
        selectedYear = c.time

        date_calendar.text = formatDate(selectedDate)

        setChipOnCheckedChangeListener()

        //Date picker
        // date
        datePicker =  MaterialDatePicker.Builder.datePicker()
            .setTitleText("SELECT A DATE")
            .setSelection(selectedDate.time)
            .build()

        // week
        datePickerForWeek = MaterialDatePicker.Builder.datePicker()
            .setTitleText("SELECT A START DATE")
            .setSelection(selectedDate.time)
            .build()

        // month
        customMonthPicker = CustomMonthPicker(selectedMonthAndYear)

        // year
        customYearPicker = CustomYearPicker(selectedYear)

        setDateToADayBeforeOrADayAfterListener()
        setDateSelectedListenerAndChangeText()

        return root
    }
    private fun startViewModelAndService(){
        val intent = Intent(requireActivity(), BackgroundDetectedActivitiesService::class.java)
        requireActivity().startService(intent)
        activityRecognitionContext = requireActivity()//this.applicationContext
        activityViewModel = ViewModelProvider(this).get(ActivityRecognitionViewModel::class.java)
        activityRecognitionContext.bindService(intent, activityViewModel, Context.BIND_AUTO_CREATE)
        println("Get into the startViewModelAndService")
    }

    private fun observeInstantActivity(it: Int){
        activityAdapter = ActivityAdapter(summaryList)
        val mLayoutManager = LinearLayoutManager(activity?.applicationContext)
        mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = activityAdapter
        val typeAmountMap: MutableMap<String, Float> = HashMap()
        val isExist = activityRecognitionDataViewModel.checkExistanceOfTheDay("%" + LocalDate.now().toString() + "%")
        if(isExist == 0) {
            if (activityViewModel.stillActivity != 0f) {
                typeAmountMap["Still"] = (((activityViewModel.stillActivity)).toFloat())
                if (stillEnterLabel == 0) {
                    enter_index += 1
                    stillSummary = SummaryModel("Still", activityViewModel.stillActivity)
                    summaryList.add(stillSummary!!)
                    stillEnterLabel = 1
                    still_index = enter_index
                } else {
                    summaryList[still_index - 1] =
                        SummaryModel("Still", activityViewModel.stillActivity)
                }
            }
            if (activityViewModel.walkActivity != 0f) {
                typeAmountMap["Walk"] = (((activityViewModel.walkActivity)).toFloat())
                if (walkEnterLabel == 0) {
                    enter_index += 1
                    walkSummary = SummaryModel("Walk", activityViewModel.walkActivity)
                    summaryList.add(walkSummary!!)
                    walkEnterLabel = 1
                    walk_index = enter_index
                } else {
                    summaryList[walk_index - 1] =
                        SummaryModel("Walk", activityViewModel.walkActivity)
                }

            }
            if (activityViewModel.runActivity != 0f) {
                typeAmountMap["Run"] = (activityViewModel.runActivity)
                if (runEnterLabel == 0) {
                    enter_index += 1
                    runSummary = SummaryModel("Run", activityViewModel.runActivity)
                    summaryList.add(runSummary!!)
                    runEnterLabel = 1
                    run_index = enter_index
                } else {
                    summaryList[run_index - 1] = SummaryModel("Run", activityViewModel.runActivity)
                }

            }
            if (activityViewModel.vehicleActivity != 0f) {
                typeAmountMap["Vehicle"] = (activityViewModel.vehicleActivity)
                if (vehicleEnterLabel == 0) {
                    enter_index += 1
                    vehicleSummary = SummaryModel("Vehicle", activityViewModel.vehicleActivity)
                    summaryList.add(vehicleSummary!!)
                    vehicleEnterLabel = 1
                    vehicle_index = enter_index
                } else {
                    summaryList[vehicle_index - 1] =
                        SummaryModel("Vehicle", activityViewModel.vehicleActivity)
                }
            }
            if (activityViewModel.bicycleActivity != 0f) {
                typeAmountMap["Bicycle"] = activityViewModel.bicycleActivity
                if (bicycleEnterLabel == 0) {
                    enter_index += 1
                    bicycleSummary = SummaryModel("Bicycle", activityViewModel.bicycleActivity)
                    summaryList.add(bicycleSummary!!)
                    bicycleEnterLabel = 1
                    bicycle_index = enter_index
                } else {
                    summaryList[bicycle_index - 1] =
                        SummaryModel("Bicycle", activityViewModel.bicycleActivity)
                }
            }
            if (activityViewModel.tiltActivity != 0f) {
                typeAmountMap["Tilt"] = activityViewModel.tiltActivity
                if (tiltEnterLabel == 0) {
                    enter_index += 1
                    tiltSummary = SummaryModel("Tilt", activityViewModel.tiltActivity)
                    summaryList.add(tiltSummary!!)
                    tiltEnterLabel = 1
                    tilt_index = enter_index
                } else {
                    summaryList[tilt_index - 1] =
                        SummaryModel("Tilt", activityViewModel.tiltActivity)
                }
            }
            if (activityViewModel.onFootActivity != 0f) {
                typeAmountMap["OnFoot"] = activityViewModel.onFootActivity
                if (onFootEnterLabel == 0) {
                    enter_index += 1
                    onFootSummary = SummaryModel("OnFoot", activityViewModel.onFootActivity)
                    summaryList.add(onFootSummary!!)
                    onFootEnterLabel = 1
                    onFoot_index = enter_index
                } else {
                    summaryList[onFoot_index - 1] =
                        SummaryModel("OnFoot", activityViewModel.onFootActivity)
                }
            }
            if (activityViewModel.unknownActivity != 0f) {
                typeAmountMap["Unknown"] = activityViewModel.unknownActivity
                if (unknownEnterLabel == 0) {
                    enter_index += 1
                    unknownSummary = SummaryModel("Unknown", activityViewModel.unknownActivity)
                    summaryList.add(unknownSummary!!)
                    unknownEnterLabel = 1
                    unknown_index = enter_index
                } else {
                    summaryList[unknown_index - 1] =
                        SummaryModel("Unknown", activityViewModel.unknownActivity)
                }
            }
            activityAdapter.notifyDataSetChanged()
            showPieChart(typeAmountMap)
        }
        else if(isExist != 0){
            val result  = activityRecognitionDataViewModel.getSingleDayActivityDuration(LocalDate.now().toString())
            if (result.StillTime != 0f || activityViewModel.stillActivity != 0f){
                typeAmountMap["Still"] = activityViewModel.stillActivity + result.StillTime
                if (stillEnterLabel == 0) {
                    enter_index += 1
                    stillSummary = SummaryModel("Still", activityViewModel.stillActivity+ result.StillTime)
                    summaryList.add(stillSummary!!)
                    stillEnterLabel = 1
                    still_index = enter_index
                } else {
                    summaryList[still_index - 1] =
                        SummaryModel("Still", activityViewModel.stillActivity+ result.StillTime)
                }
            }
            if (result.WalkTime != 0f || activityViewModel.walkActivity != 0f) {
                typeAmountMap["Walk"] = (((activityViewModel.walkActivity + result.WalkTime)).toFloat())
                if (walkEnterLabel == 0) {
                    enter_index += 1
                    walkSummary = SummaryModel("Walk", activityViewModel.walkActivity + result.WalkTime)
                    summaryList.add(walkSummary!!)
                    walkEnterLabel = 1
                    walk_index = enter_index
                } else {
                    summaryList[walk_index - 1] =
                        SummaryModel("Walk", activityViewModel.walkActivity + result.WalkTime)
                }

            }
            if (result.RunTime != 0f || activityViewModel.runActivity != 0f) {
                typeAmountMap["Run"] = (activityViewModel.runActivity + result.RunTime)
                if (runEnterLabel == 0) {
                    enter_index += 1
                    runSummary = SummaryModel("Run", activityViewModel.runActivity + result.RunTime)
                    summaryList.add(runSummary!!)
                    runEnterLabel = 1
                    run_index = enter_index
                } else {
                    summaryList[run_index - 1] = SummaryModel("Run", activityViewModel.runActivity + result.RunTime)
                }

            }
            if (result.VehicleTime != 0f || activityViewModel.vehicleActivity != 0f) {
                typeAmountMap["Vehicle"] = (activityViewModel.vehicleActivity + result.VehicleTime)
                if (vehicleEnterLabel == 0) {
                    enter_index += 1
                    vehicleSummary = SummaryModel("Vehicle", activityViewModel.vehicleActivity + result.VehicleTime)
                    summaryList.add(vehicleSummary!!)
                    vehicleEnterLabel = 1
                    vehicle_index = enter_index
                } else {
                    summaryList[vehicle_index - 1] =
                        SummaryModel("Vehicle", activityViewModel.vehicleActivity + result.VehicleTime)
                }
            }
            if (result.BicycleTime != 0f || activityViewModel.bicycleActivity != 0f) {
                typeAmountMap["Bicycle"] = activityViewModel.bicycleActivity + result.BicycleTime
                if (bicycleEnterLabel == 0) {
                    enter_index += 1
                    bicycleSummary = SummaryModel("Bicycle", activityViewModel.bicycleActivity + result.BicycleTime)
                    summaryList.add(bicycleSummary!!)
                    bicycleEnterLabel = 1
                    bicycle_index = enter_index
                } else {
                    summaryList[bicycle_index - 1] =
                        SummaryModel("Bicycle", activityViewModel.bicycleActivity + result.BicycleTime)
                }
            }
            if (result.TiltTime != 0f || activityViewModel.tiltActivity != 0f) {
                typeAmountMap["Tilt"] = activityViewModel.tiltActivity + result.TiltTime
                if (tiltEnterLabel == 0) {
                    enter_index += 1
                    tiltSummary = SummaryModel("Tilt", activityViewModel.tiltActivity + result.TiltTime)
                    summaryList.add(tiltSummary!!)
                    tiltEnterLabel = 1
                    tilt_index = enter_index
                } else {
                    summaryList[tilt_index - 1] =
                        SummaryModel("Tilt", activityViewModel.tiltActivity + result.TiltTime)
                }
            }
            if (result.OnFootTime != 0f || activityViewModel.onFootActivity != 0f) {
                typeAmountMap["OnFoot"] = activityViewModel.onFootActivity + result.OnFootTime
                if (onFootEnterLabel == 0) {
                    enter_index += 1
                    onFootSummary = SummaryModel("OnFoot", activityViewModel.onFootActivity + result.OnFootTime)
                    summaryList.add(onFootSummary!!)
                    onFootEnterLabel = 1
                    onFoot_index = enter_index
                } else {
                    summaryList[onFoot_index - 1] =
                        SummaryModel("OnFoot", activityViewModel.onFootActivity + result.OnFootTime)
                }
            }
            if (result.UnknownTime != 0f || activityViewModel.unknownActivity != 0f) {
                typeAmountMap["Unknown"] = activityViewModel.unknownActivity + result.UnknownTime
                if (unknownEnterLabel == 0) {
                    enter_index += 1
                    unknownSummary = SummaryModel("Unknown", activityViewModel.unknownActivity + result.UnknownTime)
                    summaryList.add(unknownSummary!!)
                    unknownEnterLabel = 1
                    unknown_index = enter_index
                } else {
                    summaryList[unknown_index - 1] =
                        SummaryModel("Unknown", activityViewModel.unknownActivity + result.UnknownTime)
                }
            }
            activityAdapter.notifyDataSetChanged()
            showPieChart(typeAmountMap)
        }
    }

    private fun initializeUIByDayData() {
        activityViewModel.observeActivity.observe(requireActivity(),activityServiceObserver)//{ it ->
    }

    private fun checkWeekActivityData(startTime: String, endTime:String){
        activityViewModel.observeActivity.removeObserver(activityServiceObserver)
        val typeAmountMap: MutableMap<String, Float> = HashMap()
        val historySummary = ArrayList<SummaryModel>()
        historyAdapter = ActivityAdapter(historySummary)
        val mLayoutManager = LinearLayoutManager(activity?.applicationContext)
        mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = historyAdapter
        var weekStillDuration: Float = 0F
        var weekRunDuration: Float = 0F
        var weekWalkDuration: Float = 0F
        var weekVehicleDuration: Float = 0F
        var weekBicycleDuration: Float = 0F
        var weekOnFootDuration: Float = 0F
        var weekUnknownDuration: Float = 0F
        var weekTiltDuration: Float = 0F
        var activityDurationList = getData(startTime, endTime)
        for(iter in activityDurationList){
            weekStillDuration += iter.StillTime
            weekRunDuration += iter.RunTime
            weekWalkDuration += iter.WalkTime
            weekVehicleDuration += iter.VehicleTime
            weekBicycleDuration += iter.BicycleTime
            weekOnFootDuration += iter.OnFootTime
            weekUnknownDuration += iter.UnknownTime
            weekTiltDuration += iter.TiltTime
        }
        if(weekStillDuration != 0F){
            typeAmountMap["Still"] = weekStillDuration
            historySummary.add(SummaryModel("Still", weekStillDuration))
        }
        if(weekRunDuration != 0F){
            typeAmountMap["Run"] = weekRunDuration
            historySummary.add(SummaryModel("Run", weekRunDuration))
        }
        if(weekWalkDuration != 0F){
            typeAmountMap["Walk"] = weekWalkDuration
            historySummary.add(SummaryModel("Walk", weekWalkDuration))
        }
        if(weekVehicleDuration != 0F){
            typeAmountMap["Vehicle"] = weekVehicleDuration
            historySummary.add(SummaryModel("Vehicle", weekVehicleDuration))
        }
        if(weekBicycleDuration != 0F){
            typeAmountMap["Bicycle"] = weekBicycleDuration
            historySummary.add(SummaryModel("Bicycle", weekBicycleDuration))
        }
        if(weekOnFootDuration != 0F){
            typeAmountMap["OnFoot"] = weekOnFootDuration
            historySummary.add(SummaryModel("OnFoot", weekOnFootDuration))
        }
        if(weekUnknownDuration != 0F){
            typeAmountMap["Unknown"] = weekUnknownDuration
            historySummary.add(SummaryModel("Unknown", weekUnknownDuration))
        }
        if(weekTiltDuration != 0F){
            typeAmountMap["Tilt"] = weekTiltDuration
            historySummary.add(SummaryModel("Tilt", weekTiltDuration))
        }
        println("Debug: Print the length of arraylist: ${historySummary.size}")
        showPieChart(typeAmountMap)
        historyAdapter.notifyDataSetChanged()
    }

    private fun getData(startTime: String, endTime: String): List<ActivityRecognitionEntity>{
        val results = activityRecognitionDataViewModel.getActivityDurationBetween(startTime, endTime)
        for( r in results){
            println("debug: the activity time is "+ r.Date)
        }
        return results
    }
    private fun getDataForSingleDay(dayDate: String){
        activityViewModel.observeActivity.removeObserver(activityServiceObserver)
        val typeAmountMap: MutableMap<String, Float> = HashMap()
        val historySummary = ArrayList<SummaryModel>()
        historyAdapter = ActivityAdapter(historySummary)
        val mLayoutManager = LinearLayoutManager(activity?.applicationContext)
        mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = historyAdapter
        var weekStillDuration: Float = 0F
        var weekRunDuration: Float = 0F
        var weekWalkDuration: Float = 0F
        var weekVehicleDuration: Float = 0F
        var weekBicycleDuration: Float = 0F
        var weekOnFootDuration: Float = 0F
        var weekUnknownDuration: Float = 0F
        var weekTiltDuration: Float = 0F
        val result  = activityRecognitionDataViewModel.getSingleDayActivityDuration(dayDate)
        weekStillDuration += result.StillTime
        weekRunDuration += result.RunTime
        weekWalkDuration += result.WalkTime
        weekVehicleDuration += result.VehicleTime
        weekBicycleDuration += result.BicycleTime
        weekOnFootDuration += result.OnFootTime
        weekUnknownDuration += result.UnknownTime
        weekTiltDuration += result.TiltTime

        if(weekStillDuration != 0F){
            typeAmountMap["Still"] = weekStillDuration
            historySummary.add(SummaryModel("Still", weekStillDuration))
        }
        if(weekRunDuration != 0F){
            typeAmountMap["Run"] = weekRunDuration
            historySummary.add(SummaryModel("Run", weekRunDuration))
        }
        if(weekWalkDuration != 0F){
            typeAmountMap["Walk"] = weekWalkDuration
            historySummary.add(SummaryModel("Walk", weekWalkDuration))
        }
        if(weekVehicleDuration != 0F){
            typeAmountMap["Vehicle"] = weekVehicleDuration
            historySummary.add(SummaryModel("Vehicle", weekVehicleDuration))
        }
        if(weekBicycleDuration != 0F){
            typeAmountMap["Bicycle"] = weekBicycleDuration
            historySummary.add(SummaryModel("Bicycle", weekBicycleDuration))
        }
        if(weekOnFootDuration != 0F){
            typeAmountMap["OnFoot"] = weekOnFootDuration
            historySummary.add(SummaryModel("OnFoot", weekOnFootDuration))
        }
        if(weekUnknownDuration != 0F){
            typeAmountMap["Unknown"] = weekUnknownDuration
            historySummary.add(SummaryModel("Unknown", weekUnknownDuration))
        }
        if(weekTiltDuration != 0F){
            typeAmountMap["Tilt"] = weekTiltDuration
            historySummary.add(SummaryModel("Tilt", weekTiltDuration))
        }
        showPieChart(typeAmountMap)
        historyAdapter.notifyDataSetChanged()
    }

    private fun futureDateBlank(){
        activityViewModel.observeActivity.removeObserver(activityServiceObserver)
        val typeAmountMap: MutableMap<String, Float> = HashMap()
        val historySummary = ArrayList<SummaryModel>()
        historyAdapter = ActivityAdapter(historySummary)
        val mLayoutManager = LinearLayoutManager(activity?.applicationContext)
        mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = historyAdapter
        showPieChart(typeAmountMap)
    }

    private fun setChipOnCheckedChangeListener() {
        println("Debug: Enter the check listener")
        activityChipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != -1){
                checkedChip = group.findViewById(checkedId)
                println("debug: select chip ${checkedChip.text}")
                checkedChipText = checkedChip.text.toString()
                // reshow the bar chart with new data
                if (checkedChipText == "DAY"){
                    println("Debug: DAY is selected")
                    initializeUIByDayData()
                    changeDateTextAccordingToChip("DAY")
                }else if (checkedChipText == "WEEK"){
                    println("Debug: WEEK is selected")
                    val endDate = LocalDate.now().minusDays(1).toString()
                    val startDate = LocalDate.now().minusDays(7).toString()
                    checkWeekActivityData(startDate, endDate)
                    changeDateTextAccordingToChip("WEEK")
                }else if (checkedChipText == "MONTH"){
                    println("Debug: MONTH is selected")
                    val endDate = LocalDate.now().minusDays(1).toString()
                    var tempDate = Calendar.getInstance()
                    println("Debug: tempDate is ${tempDate.toString()}")
                    val currentM = tempDate.get(Calendar.MONTH)
                    val currentY = tempDate.get(Calendar.YEAR)
                    val startDate = LocalDate.of(currentY,currentM+1,1).toString()
                    checkWeekActivityData(startDate, endDate)
                    changeDateTextAccordingToChip("MONTH")
                }else if (checkedChipText == "YEAR"){
                    println("Debug: YEAR is selected")
                    val endDate = LocalDate.now().minusDays(1).toString()
                    var tempDate = Calendar.getInstance()
                    val currentY = "%"+tempDate.get(Calendar.YEAR).toString()+"%"
                    println("Debug: currentY is $currentY")
                    val startDate = activityRecognitionDataViewModel.getEarliestActivity(currentY).Date
                    println("Debug: year startDate is $startDate")
                    checkWeekActivityData(startDate, endDate)
                    changeDateTextAccordingToChip("YEAR")
                }

            }else{
                println("debug: select the same chip, check this chip again")
                checkedChip.isChecked = true
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun changeDateTextAccordingToChip(chipText: String){
        if (chipText == "DAY"){
            date_calendar.text = formatDate(selectedDate)

        }else if (chipText == "WEEK"){
            val c = Calendar.getInstance()
            c.time = Date()
            c.add(Calendar.DATE, -1)
            val endDayDate = c.time
            c.add(Calendar.DATE, -6)
            val startDayDate = c.time

            date_calendar.text = formatDate(startDayDate) + " - " + formatDate(endDayDate)

        }else if (chipText == "MONTH"){
            date_calendar.text = formatMonth(selectedMonthAndYear)

        }else if (chipText == "YEAR"){
            date_calendar.text = formatYear((selectedYear))

        }
    }

    private fun formatDate(date: Date): String{
        val c = Calendar.getInstance()
        c.time = date
        // int start from Sunday = 1, Monday = 2, ...
        val weekDay = arrayListOf<String>("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val dayOfWeek = weekDay[c[Calendar.DAY_OF_WEEK]-1]
        val dayOfMonth = c.get(Calendar.DAY_OF_MONTH).toString()

        val month = monthName[c.get(Calendar.MONTH)]

        return "$dayOfWeek $dayOfMonth $month"
    }
    private fun formatDateForActivityDB(date: Date):String{
        val c = Calendar.getInstance()
        c.time = date
        val year = c.get(Calendar.YEAR).toString()
        val month = (c.get(Calendar.MONTH)+1)
        val day = c.get(Calendar.DAY_OF_MONTH)
        var zday: String = ""
        if(day<10){
            zday = "0${day}"
        }
        else{
            zday = "$day"
        }
        var zmonth: String = ""
        if(month<10){
            zmonth = "0${month}"
        }
        else{
            zmonth = "$month"
        }
        return "$year-$zmonth-$zday"
    }
    private fun formatMonth(date: Date): String{
        val c = Calendar.getInstance()
        c.time = date
        println("debug: c.get(Calendar.MONTH) "+ c.get(Calendar.MONTH).toString())
        val month = monthName[c.get(Calendar.MONTH)]
        val year = c.get(Calendar.YEAR).toString()
        return "$month $year"
    }
    private fun formatMonthForActivityDB(date: Date): MutableList<String>{
        val c = Calendar.getInstance()
        c.time = date
        val year = c.get(Calendar.YEAR).toString()
        val month = (c.get(Calendar.MONTH)+1)
        val day = c.get(Calendar.DAY_OF_MONTH)
        var zday: String = ""
        if(day<10){
            zday = "0${day}"
        }
        else{
            zday = "$day"
        }
        var zmonth: String = ""
        if(month<10){
            zmonth = "0${month}"
        }
        else{
            zmonth = "$month"
        }
        val twoDate: MutableList<String> = mutableListOf()
        twoDate.add("$year-$zmonth-$zday")
        val lastday = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        twoDate.add("$year-$zmonth-$lastday")
        return twoDate
    }

    private fun formatYearForActivityDB(date: Date): MutableList<String>{
        val c = Calendar.getInstance()
        c.time = date
        var year = c.get(Calendar.YEAR).toString()
        var month = (c.get(Calendar.MONTH)+1)
        var day = c.get(Calendar.DAY_OF_MONTH)
        var zday: String = ""
        if(day<10){
            zday = "0${day}"
        }
        else{
            zday = "$day"
        }
        var zmonth: String = ""
        if(month<10){
            zmonth = "0${month}"
        }
        else{
            zmonth = "$month"
        }
        val twoDate: MutableList<String> = mutableListOf()
        twoDate.add("$year-$zmonth-$zday")
        c.add(Calendar.MONTH, 11)
        c.add(Calendar.DAY_OF_MONTH, 30)
        year = c.get(Calendar.YEAR).toString()
        month = (c.get(Calendar.MONTH)+1)
        day = c.get(Calendar.DAY_OF_MONTH)
        if(day<10){
            zday = "0${day}"
        }
        else{
            zday = "$day"
        }
        if(month<10){
            zmonth = "0${month}"
        }
        else{
            zmonth = "$month"
        }

        twoDate.add("$year-$zmonth-$zday")
        println("Peek: the last day of the year is $year-$zmonth-$zday")
        return twoDate
    }



    private fun formatYear(date: Date): String{
        val c = Calendar.getInstance()
        c.time = date
        val year = c.get(Calendar.YEAR).toString()
        return "$year"
    }

    private fun setDateToADayBeforeOrADayAfterListener(){
        val c = Calendar.getInstance()
        aDayBeforeDateImageView.setOnClickListener { it ->
            if (checkedChip.text == "DAY") {
                selectedDate = Date(selectedDate.getTime() - 86400000L)
                date_calendar.text = formatDate(selectedDate)
                datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("SELECT A DATE")
                    .setSelection(selectedDate.time)
                    .build()

                val dateForRead = formatDateForActivityDB(selectedDate)
                if(dateForRead == LocalDate.now().toString()){
                    initializeUIByDayData()
                }
                val isExist = activityRecognitionDataViewModel.checkExistanceOfTheDay("%" + dateForRead + "%")
                if (isExist == 0){
                    futureDateBlank()
                }
                else if(isExist != 0){
                    getDataForSingleDay(dateForRead)
                }

            } else if (checkedChip.text == "WEEK") {
                c.time = selectedWeekStartDate
                c.add(Calendar.DATE, -1)
                selectedWeekEndDate = c.time
                c.add(Calendar.DATE, -6)
                selectedWeekStartDate = c.time

                date_calendar.text =
                    formatDate(selectedWeekStartDate) + " - " + formatDate(selectedWeekEndDate)

                datePickerForWeek = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("SELECT A START DATE")
                    .setSelection(selectedWeekStartDate.time)
                    .build()
                val earliestDate = activityRecognitionDataViewModel.getEarliestActivityWithoutYear().Date
                val eDate = LocalDate.parse(earliestDate,  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                if(eDate > selectedWeekEndDate.time){
                    futureDateBlank()
                }
                else if(eDate < selectedWeekEndDate.time && eDate > selectedWeekStartDate.time){
                    checkWeekActivityData(earliestDate, formatDateForActivityDB(selectedWeekEndDate))
                }
                else if(eDate < selectedWeekStartDate.time){
                    checkWeekActivityData(formatDateForActivityDB(selectedWeekStartDate), formatDateForActivityDB(selectedWeekEndDate))
                }



            } else if (checkedChip.text == "MONTH"){
                c.time = selectedMonthAndYear
                c.add(Calendar.MONTH, -1)
                if(!earliestC.after(c)){
                    selectedMonthAndYear = c.time
                }
                println("Debug: selectedMonthAndYear is $selectedMonthAndYear")
                date_calendar.text = formatMonth(selectedMonthAndYear)
                customMonthPicker = CustomMonthPicker(selectedMonthAndYear)
                val zdt: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.of("America/New_York"))
                val millis = zdt.toInstant().toEpochMilli()
                val earliestDate = activityRecognitionDataViewModel.getEarliestActivityWithoutYear().Date
                val eDate = LocalDate.parse(earliestDate,  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                val monthDateList = formatMonthForActivityDB(selectedMonthAndYear)
                val lastDateofMonth = LocalDate.parse(monthDateList[1],  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                if(eDate > selectedMonthAndYear.time && eDate < lastDateofMonth){
                    if (lastDateofMonth < millis) {
                        println("Test 1")
                        checkWeekActivityData(earliestDate, monthDateList[1])
                    }
                    else if (lastDateofMonth >= millis){
                        println("Test 2")
                        val endDate = LocalDate.now().minusDays(1).toString()
                        checkWeekActivityData(earliestDate, endDate)
                    }
                }
                else if(eDate > lastDateofMonth){
                    println("Test 3")
                    futureDateBlank()
                }
                else if(eDate < selectedMonthAndYear.time){
                    if(lastDateofMonth < millis){
                        println("Test 4")
                        checkWeekActivityData(monthDateList[0], monthDateList[1])
                    }
                    else if(lastDateofMonth >= millis){
                        println("Test 5")
                        val endDate = LocalDate.now().minusDays(1).toString()
                        checkWeekActivityData(monthDateList[0], endDate)
                    }
                    else if(selectedMonthAndYear.time > millis){
                        println("Test 6")
                        futureDateBlank()
                    }
                }

            } else if (checkedChip.text == "YEAR"){
                c.time = selectedYear
                c.add(Calendar.YEAR, -1)
                if(!earliestC.after(c)) {
                    selectedYear = c.time
                    date_calendar.text = c[Calendar.YEAR].toString()
                }
                customYearPicker = CustomYearPicker(selectedYear)
                val zdt: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.of("America/New_York"))
                val millis = zdt.toInstant().toEpochMilli()
                val earliestDate = activityRecognitionDataViewModel.getEarliestActivityWithoutYear().Date
                val eDate = LocalDate.parse(earliestDate,  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()

                val yearDateList = formatYearForActivityDB(selectedYear)

                val lastDateofYear = LocalDate.parse(yearDateList[1],  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                if(eDate > selectedMonthAndYear.time && eDate < lastDateofYear){
                    if (lastDateofYear < millis) {
                        checkWeekActivityData(earliestDate, yearDateList[1])
                    }
                    else if (lastDateofYear >= millis){
                        val endDate = LocalDate.now().minusDays(1).toString()
                        checkWeekActivityData(earliestDate, endDate)
                    }
                }
                else if(eDate > lastDateofYear){
                    futureDateBlank()
                }
                else if(eDate < selectedMonthAndYear.time){
                    if (lastDateofYear < millis){
                        checkWeekActivityData(yearDateList[0], yearDateList[1])
                    }
                    else if(lastDateofYear >= millis){
                        val endDate = LocalDate.now().minusDays(1).toString()
                        checkWeekActivityData(yearDateList[0], endDate)
                    }
                    else if(selectedMonthAndYear.time > millis){
                        futureDateBlank()
                    }
                }
            }
        }

        aDayAfterDateImageView.setOnClickListener{ it->
            if (checkedChip.text == "DAY"){
                selectedDate = Date(selectedDate.getTime() + 86400000L)
                date_calendar.text = formatDate(selectedDate)
                datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("SELECT A DATE")
                    .setSelection(selectedDate.time)
                    .build()
                val dateForRead = formatDateForActivityDB(selectedDate)
                println("Debug:dateForRead is $dateForRead ")
                if(dateForRead == LocalDate.now().toString()){
                    initializeUIByDayData()
                }
                val isExist = activityRecognitionDataViewModel.checkExistanceOfTheDay("%" + dateForRead + "%")
                if (isExist == 0){
                    futureDateBlank()
                }
                else if(isExist != 0){
                    getDataForSingleDay(dateForRead)
                }
            }
            else if(checkedChip.text == "WEEK"){
                c.time = selectedWeekStartDate
                c.add(Calendar.DATE, 7)
                val cal = Calendar.getInstance()
                cal.time = Date()
                if (c.after(cal)) {
                    c.time = Date()
                    c.add(Calendar.DATE, -1)
                    selectedWeekEndDate = c.time
                    c.add(Calendar.DATE, -6)
                    selectedWeekStartDate = c.time
                } else {
                    selectedWeekStartDate = c.time
                }
                c.add(Calendar.DATE, 6)
                if (c.after(cal)) {
                    c.time = Date()
                    c.add(Calendar.DATE, -1)
                    selectedWeekEndDate = c.time
                    c.add(Calendar.DATE, -6)
                    selectedWeekStartDate = c.time
                } else {
                    selectedWeekEndDate = c.time
                }

                date_calendar.text =
                    formatDate(selectedWeekStartDate) + " - " + formatDate(selectedWeekEndDate)

                datePickerForWeek = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("SELECT A START DATE")
                    .setSelection(selectedWeekStartDate.time)
                    .build()

                val zdt: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.of("America/New_York")).minusDays(1)
                val millis = zdt.toInstant().toEpochMilli()
                val earliestDate = activityRecognitionDataViewModel.getEarliestActivityWithoutYear().Date
                val eDate = LocalDate.parse(earliestDate,  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                if(eDate > selectedWeekEndDate.time){
                    futureDateBlank()
                }
                else if(eDate < selectedWeekEndDate.time && eDate > selectedWeekStartDate.time){
                    checkWeekActivityData(earliestDate, formatDateForActivityDB(selectedWeekEndDate))
                }
                else if(eDate < selectedWeekStartDate.time){
                    checkWeekActivityData(formatDateForActivityDB(selectedWeekStartDate), formatDateForActivityDB(selectedWeekEndDate))
                }

            }

            else if (checkedChip.text == "MONTH"){
                c.time = selectedMonthAndYear
                c.add(Calendar.MONTH, 1)

                val cal = Calendar.getInstance()
                cal.time = Date()
                if (c.before(cal)) {
                    selectedMonthAndYear = c.time
                }
                date_calendar.text =formatMonth(selectedMonthAndYear)

                customMonthPicker = CustomMonthPicker(selectedMonthAndYear)

                val zdt: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.of("America/New_York"))
                val millis = zdt.toInstant().toEpochMilli()
                val earliestDate = activityRecognitionDataViewModel.getEarliestActivityWithoutYear().Date
                val eDate = LocalDate.parse(earliestDate,  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                val monthDateList = formatMonthForActivityDB(selectedMonthAndYear)
                val lastDateofMonth = LocalDate.parse(monthDateList[1],  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                if(eDate > selectedMonthAndYear.time && eDate < lastDateofMonth){
                    if (lastDateofMonth < millis) {
                        checkWeekActivityData(earliestDate, monthDateList[1])
                    }
                    else if (lastDateofMonth >= millis){
                        val endDate = LocalDate.now().minusDays(1).toString()
                        checkWeekActivityData(earliestDate, endDate)
                    }
                }
                else if(eDate > lastDateofMonth){
                    futureDateBlank()
                }
                else if(eDate < selectedMonthAndYear.time){
                    if(lastDateofMonth < millis){
                        checkWeekActivityData(monthDateList[0], monthDateList[1])
                    }
                    else if(lastDateofMonth >= millis){
                        val endDate = LocalDate.now().minusDays(1).toString()
                        checkWeekActivityData(monthDateList[0], endDate)
                    }
                    else if(selectedMonthAndYear.time > millis){
                        futureDateBlank()
                    }
                }

            }

            else if (checkedChip.text == "YEAR"){

                c.time = selectedYear
                c.add(Calendar.YEAR, 1)

                val cal = Calendar.getInstance()
                cal.time = Date()
                println("debug: " + c.time.toString())
                println("debug: " + cal.time.toString())
                println("debug: " + c.before(cal).toString())


                if (c.before(cal)) {
                    selectedYear = c.time
                }else{
                    c.time = selectedYear
                }

                date_calendar.text = c[Calendar.YEAR].toString()

                customYearPicker = CustomYearPicker(selectedYear)

                val zdt: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.of("America/New_York"))
                val millis = zdt.toInstant().toEpochMilli()
                val earliestDate = activityRecognitionDataViewModel.getEarliestActivityWithoutYear().Date
                val eDate = LocalDate.parse(earliestDate,  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()

                val yearDateList = formatYearForActivityDB(selectedYear)

                val lastDateofYear = LocalDate.parse(yearDateList[1],  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                if(eDate > selectedMonthAndYear.time && eDate < lastDateofYear){
                    if (lastDateofYear < millis) {
                        checkWeekActivityData(earliestDate, yearDateList[1])
                    }
                    else if (lastDateofYear >= millis){
                        val endDate = LocalDate.now().minusDays(1).toString()
                        checkWeekActivityData(earliestDate, endDate)
                    }
                }
                else if(eDate > lastDateofYear){
                    futureDateBlank()
                }
                else if(eDate < selectedMonthAndYear.time){
                    if (lastDateofYear < millis){
                        checkWeekActivityData(yearDateList[0], yearDateList[1])
                    }
                    else if(lastDateofYear >= millis){
                        val endDate = LocalDate.now().minusDays(1).toString()
                        checkWeekActivityData(yearDateList[0], endDate)
                    }
                    else if(selectedMonthAndYear.time > millis){
                        futureDateBlank()
                    }
                }

            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setDateSelectedListenerAndChangeText(){
        date_calendar.setOnClickListener { it ->
            if (checkedChip.text.toString() == "DAY"){
                if(!datePicker.isAdded){
                    datePicker.show(requireActivity().supportFragmentManager, "MATERIAL_DATE_PICKER")
                    datePicker.addOnPositiveButtonClickListener { it ->
                        val timeZoneUTC: TimeZone = TimeZone.getDefault()
                        val offsetFromUTC: Int = timeZoneUTC.getOffset(Date().getTime()) * -1
                        val selectedC = Calendar.getInstance()
                        selectedC.time = Date(it + offsetFromUTC)
                        if(!earliestC.after(selectedC)) {

                            selectedDate = Date(it + offsetFromUTC)

                            date_calendar.text = formatDate(selectedDate)
                        }
                        val dateForRead = formatDateForActivityDB(selectedDate)
                        println("Debug:dateForRead is $dateForRead ")
                        if(dateForRead == LocalDate.now().toString()){
                            initializeUIByDayData()
                        }
                        val isExist = activityRecognitionDataViewModel.checkExistanceOfTheDay("%" + dateForRead + "%")
                        if (isExist == 0){
                            futureDateBlank()
                        }
                        else if(isExist != 0){
                            getDataForSingleDay(dateForRead)
                        }
                    }
                }
            }
            else if (checkedChip.text.toString() == "WEEK"){
                if(!datePickerForWeek.isAdded){
                    datePickerForWeek.show(requireActivity().supportFragmentManager, "MATERIAL_DATE_WEEK_PICKER")
                    datePickerForWeek.addOnPositiveButtonClickListener { it ->

                        val timeZoneUTC: TimeZone = TimeZone.getDefault()
                        val offsetFromUTC: Int = timeZoneUTC.getOffset(Date().getTime()) * -1

                        val selectedC = Calendar.getInstance()
                        selectedC.time = Date(it + offsetFromUTC)
                        if(!earliestC.after(selectedC)) {

                            selectedWeekStartDate = Date(it + offsetFromUTC)
                            val c = Calendar.getInstance()
                            c.time = selectedWeekStartDate
                            c.add(Calendar.DATE, 6)
                            val selectedWeekEndDate = c.time

                            date_calendar.text =
                                formatDate(selectedWeekStartDate) + " - " + formatDate(
                                    selectedWeekEndDate
                                )
                            checkWeekActivityData(formatDateForActivityDB(selectedWeekStartDate),
                                formatDateForActivityDB(selectedWeekEndDate))

                        }

                    }
                }
            }
            else if (checkedChip.text.toString() == "MONTH"){
                customMonthPicker.show(requireActivity().supportFragmentManager, "MonthPickerDialog")
                customMonthPicker.setListener(DatePickerDialog.OnDateSetListener { datePicker, i, i2, i3 ->
                    println("debug: year: $i, month: $i2")
                    val c = Calendar.getInstance()
                    c.set(Calendar.YEAR, i)
                    c.set(Calendar.MONTH, i2 - 1)
                    c.set(Calendar.DATE, 1)
                    c.set(Calendar.HOUR_OF_DAY, 0)
                    c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)

                    if(!earliestC.after(c)) {
                        selectedMonthAndYear = c.time

                        date_calendar.text = formatMonth(selectedMonthAndYear)
                    }
                    customMonthPicker = CustomMonthPicker(selectedMonthAndYear)

                    val zdt: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.of("America/New_York"))
                    val millis = zdt.toInstant().toEpochMilli()
                    val earliestDate = activityRecognitionDataViewModel.getEarliestActivityWithoutYear().Date
                    val eDate = LocalDate.parse(earliestDate,  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                    val monthDateList = formatMonthForActivityDB(selectedMonthAndYear)
                    val lastDateofMonth = LocalDate.parse(monthDateList[1],  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                    if(eDate > selectedMonthAndYear.time && eDate < lastDateofMonth){
                        if (lastDateofMonth < millis) {
                            checkWeekActivityData(earliestDate, monthDateList[1])
                        }
                        else if (lastDateofMonth >= millis){
                            val endDate = LocalDate.now().minusDays(1).toString()
                            checkWeekActivityData(earliestDate, endDate)
                        }
                    }
                    else if(eDate > lastDateofMonth){
                        futureDateBlank()
                    }
                    else if(eDate < selectedMonthAndYear.time){
                        if(lastDateofMonth < millis){
                            checkWeekActivityData(monthDateList[0], monthDateList[1])
                        }
                        else if(lastDateofMonth >= millis){
                            val endDate = LocalDate.now().minusDays(1).toString()
                            checkWeekActivityData(monthDateList[0], endDate)
                        }
                        else if(selectedMonthAndYear.time > millis){
                            futureDateBlank()
                        }
                    }


                })
            }

            else if (checkedChip.text.toString() == "YEAR"){
                customYearPicker.show(requireActivity().supportFragmentManager, "YearPickerDialog")
                customYearPicker.setListener(DatePickerDialog.OnDateSetListener { datePicker, i, i2, i3 ->
                    println("debug: year: $i")
                    val c = Calendar.getInstance()
                    c.set(Calendar.YEAR, i)
                    c.set(Calendar.MONTH, 0)
                    c.set(Calendar.DATE, 1)
                    c.set(Calendar.HOUR_OF_DAY, 0)
                    c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    if(!earliestC.after(c)) {
                        selectedYear = c.time
                        println("debug: line 438 " + c.time.toString())
                        date_calendar.text = formatYear(selectedYear)
                    }

                    customYearPicker = CustomYearPicker(selectedYear)

                    val zdt: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.of("America/New_York"))
                    val millis = zdt.toInstant().toEpochMilli()
                    val earliestDate = activityRecognitionDataViewModel.getEarliestActivityWithoutYear().Date
                    val eDate = LocalDate.parse(earliestDate,  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()

                    val yearDateList = formatYearForActivityDB(selectedYear)

                    val lastDateofYear = LocalDate.parse(yearDateList[1],  DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.of("America/New_York")).toInstant().toEpochMilli()
                    if(eDate > selectedMonthAndYear.time && eDate < lastDateofYear){
                        if (lastDateofYear < millis) {
                            checkWeekActivityData(earliestDate, yearDateList[1])
                        }
                        else if (lastDateofYear >= millis){
                            val endDate = LocalDate.now().minusDays(1).toString()
                            checkWeekActivityData(earliestDate, endDate)
                        }
                    }
                    else if(eDate > lastDateofYear){
                        futureDateBlank()
                    }
                    else if(eDate < selectedMonthAndYear.time){
                        if (lastDateofYear < millis){
                            checkWeekActivityData(yearDateList[0], yearDateList[1])
                        }
                        else if(lastDateofYear >= millis){
                            val endDate = LocalDate.now().minusDays(1).toString()
                            checkWeekActivityData(yearDateList[0], endDate)
                        }
                        else if(selectedMonthAndYear.time > millis){
                            futureDateBlank()
                        }
                    }

                })

            }

        }
    }


    private fun initPieChart() {
        //using percentage as values instead of amount
        pieChart!!.setUsePercentValues(true)

        //remove the description label on the lower left corner, default true if not set
        pieChart!!.description.isEnabled = false

        //enabling the user to rotate the chart, default true
        pieChart!!.isRotationEnabled = true
        //adding friction when rotating the pie chart
        pieChart!!.dragDecelerationFrictionCoef = 0.9f
        //setting the first entry start from right hand side, default starting from top
        pieChart!!.rotationAngle = 0f

        //highlight the entry when it is tapped, default true if not set
        pieChart!!.isHighlightPerTapEnabled = true
        //adding animation so the entries pop up from 0 degree
        pieChart!!.animateY(1400, Easing.EaseInOutQuad)//EasingOption.EaseInOutQuad
        //setting the color of the hole in the middle, default white
        //Set the middle hole
        pieChart!!.setHoleColor(Color.parseColor("#FFFFFF"))
        pieChart!!.setDrawHoleEnabled(true)
        //pieChart!!.setCenterText("Sunday")
        pieChart!!.setCenterTextSize(15f)
        pieChart!!.setCenterTextColor(Color.parseColor("#894395"))
        pieChart!!.setEntryLabelColor(Color.parseColor("#000000"))
        //pieChart!!.extraBottomOffset = 20f;
        pieChart!!.extraLeftOffset = 29f;
        pieChart!!.extraRightOffset = 29f;
        pieChart!!.legend.isWordWrapEnabled = true;

    }

    private fun showPieChart(typeAmountMap: MutableMap<String, Float>) {
        val pieEntries: ArrayList<PieEntry> = ArrayList()
        val label = "type"

        //initializing colors for the entries
        val colors: ArrayList<Int> = ArrayList()
        colors.add(Color.parseColor("#EDCBF2"))
        colors.add(Color.parseColor("#E987D9"))
        colors.add(Color.parseColor("#894395"))
        colors.add(Color.parseColor("#F9F7FC"))
        colors.add(Color.parseColor("#745E85"))
        colors.add(Color.parseColor("#4C3061"))
        colors.add(Color.parseColor("#CF9FFF"))
        colors.add(Color.parseColor("#9F2B68"))


        //input data and fit data into pie chart entry
        for (type in typeAmountMap.keys) {
            pieEntries.add(PieEntry(typeAmountMap[type]!!.toFloat(), type))
        }


        //collecting the entries with label name
        val pieDataSet = PieDataSet(pieEntries, label)
        //setting text size of the value
        pieDataSet.valueTextSize = 11f
        //providing color list for coloring different entries

        pieDataSet.colors = colors
        pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        pieDataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        pieDataSet.sliceSpace = 2f

        //grouping the data set from entry to chart
        val pieData = PieData(pieDataSet)
        //showing the value of the entries, default true if not set
        pieData.setDrawValues(true)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieData.setValueTextColor(Color.parseColor("#4C3061"))

        pieChart!!.data = pieData
        pieChart!!.invalidate()
    }

    fun checkPermission() {
        if (Build.VERSION.SDK_INT < 23) return
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
//                requireActivity(),
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                PERMISSION_REQUEST_CODE
            )
            println("Check permission run ${Build.VERSION.SDK_INT}")
        }
        else {startViewModelAndService()
            initializeUIByDayData()}

    }

    //Request Sensors permissiony
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        println("RequestCode is $requestCode")
        if (requestCode == PERMISSION_REQUEST_CODE) {
            println("RequestCode is $requestCode")
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                println("Condition is ${grantResults[0] == PackageManager.PERMISSION_GRANTED}")
                startViewModelAndService()
                initializeUIByDayData()
            }
        }
    }

    companion object {
        val BROADCAST_DETECTED_ACTIVITY = "activity_intent"
        internal val DETECTION_INTERVAL_IN_MILLISECONDS: Long = 1000
        val CONFIDENCE = 70
    }

    override fun onResume() {
        super.onResume()
        println("onResume")
//        initializeUIByDayData()
//        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(broadcastReceiver,
//            IntentFilter(ActivityRecognition.BROADCAST_DETECTED_ACTIVITY)
//        )
    }
    override fun onPause() {
        super.onPause()
        //LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(broadcastReceiver)
    }

    private fun saveTodayData(){
        val isExist = activityRecognitionDataViewModel.checkExistanceOfTheDay("%" + LocalDate.now().toString() + "%")
        if(isExist == 0) {
            val activityForOneDay = ActivityRecognitionEntity()
            activityForOneDay.Date = LocalDate.now().toString()
            activityForOneDay.StillTime = activityViewModel.stillActivity
            activityForOneDay.BicycleTime = activityViewModel.bicycleActivity
            activityForOneDay.OnFootTime = activityViewModel.onFootActivity
            activityForOneDay.WalkTime = activityViewModel.walkActivity
            activityForOneDay.RunTime = activityViewModel.runActivity
            activityForOneDay.VehicleTime = activityViewModel.vehicleActivity
            activityForOneDay.TiltTime = activityViewModel.tiltActivity
            activityForOneDay.UnknownTime = activityViewModel.unknownActivity
            repository.insert(activityForOneDay)
        }
        else if(isExist != 0){
            val result  = activityRecognitionDataViewModel.getSingleDayActivityDuration(LocalDate.now().toString())
            activityRecognitionDataViewModel.updateStillDataOfToday(result.StillTime + activityViewModel.stillActivity, LocalDate.now().toString())
            activityRecognitionDataViewModel.updateWalkDataOfToday(result.WalkTime + activityViewModel.walkActivity, LocalDate.now().toString())
            activityRecognitionDataViewModel.updateRunDataOfToday(result.RunTime + activityViewModel.runActivity, LocalDate.now().toString())
            activityRecognitionDataViewModel.updateVehicleDataOfToday(result.VehicleTime + activityViewModel.vehicleActivity, LocalDate.now().toString())
            activityRecognitionDataViewModel.updateBicycleDataOfToday(result.BicycleTime + activityViewModel.bicycleActivity, LocalDate.now().toString())
            activityRecognitionDataViewModel.updateOnFootDataOfToday(result.OnFootTime + activityViewModel.onFootActivity, LocalDate.now().toString())
            activityRecognitionDataViewModel.updateTiltDataOfToday(result.TiltTime + activityViewModel.tiltActivity, LocalDate.now().toString())
            activityRecognitionDataViewModel.updateUnknownDataOfToday(result.UnknownTime + activityViewModel.unknownActivity, LocalDate.now().toString())
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveTodayData()
        _binding = null
    }
}