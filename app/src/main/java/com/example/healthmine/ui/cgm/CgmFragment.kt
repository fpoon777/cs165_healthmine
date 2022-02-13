package com.example.healthmine.ui.cgm

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.healthmine.R
import com.example.healthmine.database.EgvDao
import com.example.healthmine.database.EgvRepository
import com.example.healthmine.database.HealthmineDatabase
import com.example.healthmine.databinding.FragmentCgmBinding
import com.example.healthmine.models.AverageEgv
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition

import com.github.mikephil.charting.components.LimitLine




class CgmFragment: Fragment() {
    private var _binding: FragmentCgmBinding? = null
    private val binding get() = _binding!!
    private lateinit var cgmBarChartCustom: CustomRoundedBarChart

    // DAY/WEEK/MONTH/YEAR chips groups
    private lateinit var cgmChipGroup: ChipGroup
    // current checked chip
    private lateinit var checkedChip: Chip
    // currently checked chip's text
    private lateinit var checkedChipText: String

    // TextView to show selected date/current date
    private lateinit var dateTextView: TextView
    // pickers
    private lateinit var datePicker: MaterialDatePicker<Long>
    private lateinit var datePickerForWeek: MaterialDatePicker<Long>
    private lateinit var customMonthPicker: CustomMonthPicker
    private lateinit var customYearPicker: CustomYearPicker
    // current selected date
    private lateinit var selectedDate : Date
    private lateinit var selectedWeekStartDate: Date
    private lateinit var selectedWeekEndDate: Date
    private lateinit var selectedMonthAndYear: Date
    private lateinit var selectedYear: Date
    // format style of date to string
    private lateinit var aDayBeforeDateImageView : ImageView
    private lateinit var aDayAfterDateImageView : ImageView
    private lateinit var earliestC: Calendar

    private lateinit var cgmDataForDay : List<AverageEgv>
    private lateinit var cgmDataForWeek : List<AverageEgv>
    private lateinit var cgmDataForMonth : List<AverageEgv>
    private lateinit var cgmDataForYear : List<AverageEgv>

    private val monthName = arrayListOf<String>("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    private lateinit var root: View

    private lateinit var database: HealthmineDatabase
    private lateinit var databaseDao: EgvDao
    private lateinit var repository: EgvRepository
    private lateinit var viewModelFactory: CgmViewModelFactory
    private lateinit var cgmViewModel: CgmViewModel

    private var DAY = "Day"
    private var WEEK = "Week"
    private var MONTH = "Month"
    private var YEAR = "Year"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentCgmBinding.inflate(inflater, container, false)
        root = binding.root

        // initialize data
        initOriginalData()

        // something to do with chips
        setChipOnCheckedChangeListener()

        // something to do with date picker
        setDateSelectedListenerAndChangeText()
        setDateToADayBeforeOrADayAfterListener()

        // something to do with chart
        showCgmBarChart(cgmDataForDay)
        setAppearanceOfCgmBarChart(cgmBarChartCustom)

        drawAvgLimitLine(cgmBarChartCustom, cgmDataForDay)

        drawLegend()

        return root
    }

    private fun drawLegend(){
        // for low
        val bitmapLow = Bitmap.createBitmap(20,20, Bitmap.Config.ARGB_8888 )
        val canvasLow = Canvas(bitmapLow)
        val legendLowImageView = root.findViewById<ImageView>(R.id.bar_chart_legend_low)

        canvasLow.drawColor(Color.parseColor("#EDCBF2"))

        val paintLow = Paint()
        paintLow.setStyle(Paint.Style.FILL)
        paintLow.setColor(Color.parseColor("#EDCBF2"))
        paintLow.setAntiAlias(true)

        legendLowImageView.setImageBitmap(bitmapLow)

        // for medium
        val bitmapMedium = Bitmap.createBitmap(20,20, Bitmap.Config.ARGB_8888 )
        val canvasMedium = Canvas(bitmapMedium)
        val legendMediumImageView = root.findViewById<ImageView>(R.id.bar_chart_legend_medium)

        canvasMedium.drawColor(Color.parseColor("#E987D9"))

        val paintMedium = Paint()
        paintMedium.setStyle(Paint.Style.FILL)
        paintMedium.setColor(Color.parseColor("#E987D9"))
        paintMedium.setAntiAlias(true)

        legendMediumImageView.setImageBitmap(bitmapMedium)

        // for high
        val bitmapHigh = Bitmap.createBitmap(20,20, Bitmap.Config.ARGB_8888 )
        val canvasHigh = Canvas(bitmapHigh)
        val legendHighImageView = root.findViewById<ImageView>(R.id.bar_chart_legend_high)

        canvasHigh.drawColor(Color.parseColor("#894395"))

        val paintHigh = Paint()
        paintHigh.setStyle(Paint.Style.FILL)
        paintHigh.setColor(Color.parseColor("#894395"))
        paintHigh.setAntiAlias(true)

        legendHighImageView.setImageBitmap(bitmapHigh)
    }

    private fun initOriginalData(){
        // --------------------------------------set chips
        cgmChipGroup = root.findViewById<View>(R.id.cgm_chip_group) as ChipGroup
        checkedChip = cgmChipGroup.findViewById(R.id.chip_day)

        // --------------------------------------set database and view model
        database = HealthmineDatabase.getInstance(requireActivity())
        databaseDao = database.egvDao
        repository = EgvRepository(databaseDao)
        viewModelFactory = CgmViewModelFactory(repository)
        cgmViewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(CgmViewModel::class.java)

        // ---------------------------------------set  Text view
        // date
        earliestC = Calendar.getInstance()
        earliestC.set(Calendar.YEAR, 2018)
        earliestC.set(Calendar.MONTH, 0)
        earliestC.set(Calendar.DATE, 1)
        earliestC.set(Calendar.HOUR_OF_DAY, 0)
        earliestC.set(Calendar.MINUTE, 0)
        earliestC.set(Calendar.SECOND, 0)
        earliestC.set(Calendar.MILLISECOND, 0)

        selectedDate = Date()

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

        // text view
        dateTextView = root.findViewById(R.id.selected_date)
        dateTextView.text = formatDate(selectedDate)
        aDayBeforeDateImageView = root.findViewById<ImageView>(R.id.cgm_date_picker_chevron_left)
        aDayAfterDateImageView = root.findViewById<ImageView>(R.id.cgm_date_picker_chevron_right)

        // ----------------------------------------date pickers
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

        // ----------------------------------------cgm bar chart
        cgmBarChartCustom = root.findViewById<View>(R.id.cgm_bar_chart) as CustomRoundedBarChart

        // ----------------------------------------set data for bar chart
        cgmDataForDay = getDayDataFromSelectedDate(selectedDate)
        cgmDataForWeek = getWeekDataFromSelectedStartDate(selectedWeekStartDate)
        cgmDataForMonth = getMonthDataFromSelectedStartDate(selectedMonthAndYear)
        cgmDataForYear = getYearDataFromSelectedStartDate(selectedYear)
    }

    private fun getData(tag: Int, startTime: Date, endTime: Date): List<AverageEgv>{
        val startLocalDateTime: LocalDateTime = LocalDateTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault())
        val endLocalDateTime : LocalDateTime = LocalDateTime.ofInstant(endTime.toInstant(), ZoneId.systemDefault())
        val results = cgmViewModel.getAvgEgvsBetween(tag, startLocalDateTime, endLocalDateTime)
        for( r in results){
            println("debug: AverageEgv systemTime "+ r.systemTime.toString() + " value "+ r.value.toString())
        }
        return results
    }

    private fun getDayDataFromSelectedDate(selectedDate: Date): List<AverageEgv>{
        val c = Calendar.getInstance()
        c.time = selectedDate
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val startTimeForDay = c.time
        c.add(Calendar.DATE, 1)
        val endTimeForDay = c.time

        return getData(0, startTimeForDay, endTimeForDay)
    }

    private fun getWeekDataFromSelectedStartDate(selectedStartDate: Date): List<AverageEgv>{
        val c = Calendar.getInstance()
        c.time = selectedStartDate
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val startTimeForDay = c.time
        c.add(Calendar.DATE, 7)
        val endTimeForDay = c.time

        return getData(1, startTimeForDay, endTimeForDay)
    }

    private fun getMonthDataFromSelectedStartDate(selectedStartDate: Date): List<AverageEgv>{
        val c = Calendar.getInstance()
        c.time = selectedStartDate
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val startTimeForDay = c.time
        c.add(Calendar.MONTH, 1)
        val endTimeForDay = c.time

        println("debug: Month ${startTimeForDay.toString()}  ${endTimeForDay.toString()} ")

        return getData(1, startTimeForDay, endTimeForDay)
    }

    private fun getYearDataFromSelectedStartDate(selectedStartDate: Date): List<AverageEgv>{
        val c = Calendar.getInstance()
        c.time = selectedStartDate
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val startTimeForDay = c.time
        c.add(Calendar.YEAR, 1)

        val endTimeForDay = c.time

        println("debug: Year ${startTimeForDay.toString()}  ${endTimeForDay.toString()} ")

        return getData(2, startTimeForDay, endTimeForDay)
    }

    private fun setDateToADayBeforeOrADayAfterListener(){
        val c = Calendar.getInstance()
        aDayBeforeDateImageView.setOnClickListener { it ->
            if (checkedChip.text == DAY) {
                selectedDate = Date(selectedDate.getTime() - 86400000L)
                dateTextView.text = formatDate(selectedDate)
                datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("SELECT A DATE")
                    .setSelection(selectedDate.time)
                    .build()
                cgmDataForDay = getDayDataFromSelectedDate(selectedDate)
                renewDataSummary(cgmDataForDay)
                drawAvgLimitLine(cgmBarChartCustom, cgmDataForDay)
                showCgmBarChart(cgmDataForDay)
                cgmBarChartCustom.invalidate()
                cgmBarChartCustom.setVisibleXRange(16f,16f)
            } else if (checkedChip.text == WEEK) {
                c.time = selectedWeekStartDate
                c.add(Calendar.DATE, -1)
                selectedWeekEndDate = c.time
                c.add(Calendar.DATE, -6)
                selectedWeekStartDate = c.time

                dateTextView.text =
                    formatDate(selectedWeekStartDate) + " - " + formatDate(selectedWeekEndDate)

                datePickerForWeek = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("SELECT A START DATE")
                    .setSelection(selectedWeekStartDate.time)
                    .build()

                cgmDataForWeek = getWeekDataFromSelectedStartDate(selectedWeekStartDate)
                renewDataSummary(cgmDataForWeek)
                drawAvgLimitLine(cgmBarChartCustom, cgmDataForWeek)
                showCgmBarChart(cgmDataForWeek)
                cgmBarChartCustom.invalidate()
                cgmBarChartCustom.setVisibleXRange(7f,7f)

            } else if (checkedChip.text == MONTH){
                c.time = selectedMonthAndYear
                c.add(Calendar.MONTH, -1)
                if(!earliestC.after(c)){
                    selectedMonthAndYear = c.time
                }
                dateTextView.text = formatMonth(selectedMonthAndYear)

                customMonthPicker = CustomMonthPicker(selectedMonthAndYear)

                cgmDataForMonth = getMonthDataFromSelectedStartDate(selectedMonthAndYear)
                renewDataSummary(cgmDataForMonth)
                drawAvgLimitLine(cgmBarChartCustom, cgmDataForMonth)
                showCgmBarChart(cgmDataForMonth)
                cgmBarChartCustom.invalidate()
                cgmBarChartCustom.setVisibleXRange(16f,16f)
            } else if (checkedChip.text == YEAR){
                c.time = selectedYear
                c.add(Calendar.YEAR, -1)
                if(!earliestC.after(c)) {
                    selectedYear = c.time
                    dateTextView.text = c[Calendar.YEAR].toString()
                }
                customYearPicker = CustomYearPicker(selectedYear)
                cgmDataForYear = getYearDataFromSelectedStartDate(selectedYear)
                renewDataSummary(cgmDataForYear)
                drawAvgLimitLine(cgmBarChartCustom, cgmDataForYear)
                showCgmBarChart(cgmDataForYear)
                cgmBarChartCustom.invalidate()
                cgmBarChartCustom.setVisibleXRange(10f,10f)
            }
        }

        aDayAfterDateImageView.setOnClickListener{ it->
            if (checkedChip.text == DAY){
                selectedDate = Date(selectedDate.getTime() + 86400000L)
                dateTextView.text = formatDate(selectedDate)
                datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("SELECT A DATE")
                    .setSelection(selectedDate.time)
                    .build()
                cgmDataForDay = getDayDataFromSelectedDate(selectedDate)
                renewDataSummary(cgmDataForDay)
                drawAvgLimitLine(cgmBarChartCustom, cgmDataForDay)
                showCgmBarChart(cgmDataForDay)
                cgmBarChartCustom.invalidate()
                cgmBarChartCustom.setVisibleXRange(16f,16f)
            }
            else if(checkedChip.text == WEEK){
                c.time = selectedWeekStartDate
                c.add(Calendar.DATE, 7)
                val cal = Calendar.getInstance()
                cal.time = Date()
                if (c.after(cal)) {
                    c.time = Date()
                    selectedWeekEndDate = Date()
                    c.add(Calendar.DATE, -6)
                    selectedWeekStartDate = c.time
                } else {
                    selectedWeekStartDate = c.time
                }
                c.add(Calendar.DATE, 6)
                if (c.after(cal)) {
                    selectedWeekEndDate = Date()
                    c.time = Date()
                    c.add(Calendar.DATE, -6)
                    selectedWeekStartDate = c.time
                } else {
                    selectedWeekEndDate = c.time
                }

                dateTextView.text =
                    formatDate(selectedWeekStartDate) + " - " + formatDate(selectedWeekEndDate)

                datePickerForWeek = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("SELECT A START DATE")
                    .setSelection(selectedWeekStartDate.time)
                    .build()
                cgmDataForWeek = getWeekDataFromSelectedStartDate(selectedWeekStartDate)
                renewDataSummary(cgmDataForWeek)
                drawAvgLimitLine(cgmBarChartCustom, cgmDataForWeek)
                showCgmBarChart(cgmDataForWeek)
                cgmBarChartCustom.invalidate()
                cgmBarChartCustom.setVisibleXRange(7f,7f)
            }

            else if (checkedChip.text == MONTH){
                c.time = selectedMonthAndYear
                c.add(Calendar.MONTH, 1)

                val cal = Calendar.getInstance()
                cal.time = Date()
                if (c.before(cal)) {
                    selectedMonthAndYear = c.time
                }
                dateTextView.text =formatMonth(selectedMonthAndYear)

                customMonthPicker = CustomMonthPicker(selectedMonthAndYear)
                cgmDataForMonth = getMonthDataFromSelectedStartDate(selectedMonthAndYear)
                renewDataSummary(cgmDataForMonth)
                drawAvgLimitLine(cgmBarChartCustom, cgmDataForMonth)
                showCgmBarChart(cgmDataForMonth)
                cgmBarChartCustom.invalidate()
                cgmBarChartCustom.setVisibleXRange(16f,16f)
            }

            else if (checkedChip.text == YEAR){

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

                dateTextView.text = c[Calendar.YEAR].toString()

                customYearPicker = CustomYearPicker(selectedYear)
                cgmDataForYear = getYearDataFromSelectedStartDate(selectedYear)
                renewDataSummary(cgmDataForYear)
                drawAvgLimitLine(cgmBarChartCustom, cgmDataForYear)
                showCgmBarChart(cgmDataForYear)
                cgmBarChartCustom.invalidate()
                cgmBarChartCustom.setVisibleXRange(10f,10f)
            }
        }
    }

    private fun setDateSelectedListenerAndChangeText(){
        dateTextView.setOnClickListener { it ->
            if (checkedChip.text.toString() == DAY){
                if(!datePicker.isAdded){
                    datePicker.show(requireActivity().supportFragmentManager, "MATERIAL_DATE_PICKER")
                    datePicker.addOnPositiveButtonClickListener { it ->
                        val timeZoneUTC: TimeZone = TimeZone.getDefault()
                        val offsetFromUTC: Int = timeZoneUTC.getOffset(Date().getTime()) * -1
                        val selectedC = Calendar.getInstance()
                        selectedC.time = Date(it + offsetFromUTC)
                        if(!earliestC.after(selectedC)) {

                            selectedDate = Date(it + offsetFromUTC)

                            dateTextView.text = formatDate(selectedDate)
                        }
                        cgmDataForDay = getDayDataFromSelectedDate(selectedDate)
                        renewDataSummary(cgmDataForDay)
                        drawAvgLimitLine(cgmBarChartCustom, cgmDataForDay)
                        showCgmBarChart(cgmDataForDay)
                        cgmBarChartCustom.invalidate()
                        cgmBarChartCustom.setVisibleXRange(16f,16f)
                    }
                }
            }
            else if (checkedChip.text.toString() == WEEK){
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

                            dateTextView.text =
                                formatDate(selectedWeekStartDate) + " - " + formatDate(
                                    selectedWeekEndDate
                                )
                        }
                        cgmDataForWeek = getWeekDataFromSelectedStartDate(selectedWeekStartDate)
                        renewDataSummary(cgmDataForWeek)
                        drawAvgLimitLine(cgmBarChartCustom, cgmDataForWeek)
                        showCgmBarChart(cgmDataForWeek)
                        cgmBarChartCustom.invalidate()
                        cgmBarChartCustom.setVisibleXRange(7f,7f)
                    }
                }
            }
            else if (checkedChip.text.toString() == MONTH){
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

                        dateTextView.text = formatMonth(selectedMonthAndYear)
                    }
                    customMonthPicker = CustomMonthPicker(selectedMonthAndYear)

                    cgmDataForMonth = getMonthDataFromSelectedStartDate(selectedMonthAndYear)
                    renewDataSummary(cgmDataForMonth)
                    drawAvgLimitLine(cgmBarChartCustom, cgmDataForMonth)
                    showCgmBarChart(cgmDataForMonth)
                    cgmBarChartCustom.invalidate()
                    cgmBarChartCustom.setVisibleXRange(16f,16f)
                })
            }

            else if (checkedChip.text.toString() == YEAR){
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
                        dateTextView.text = formatYear(selectedYear)
                    }

                    customYearPicker = CustomYearPicker(selectedYear)

                    cgmDataForYear = getYearDataFromSelectedStartDate(selectedYear)
                    renewDataSummary(cgmDataForYear)
                    drawAvgLimitLine(cgmBarChartCustom, cgmDataForYear)
                    showCgmBarChart(cgmDataForYear)
                    cgmBarChartCustom.invalidate()
                    cgmBarChartCustom.setVisibleXRange(10f,10f)
                })

            }

        }
    }

    private fun renewDataSummary(dataList: List<AverageEgv>){
        val cgmAvgTv = root.findViewById<TextView>(R.id.cgm_avg_tv)
        val cgmMinTv = root.findViewById<TextView>(R.id.cgm_min_tv)
        val cgmMaxTv = root.findViewById<TextView>(R.id.cgm_max_tv)

        val levels = arrayListOf<Int>()
        for (d in dataList){
            if(d.value != null){
                levels.add(d.value!!)
            }
        }
        cgmAvgTv.text = levels.average().toInt().toString()
        cgmMaxTv.text = (levels.maxOrNull() ?: 0).toString()
        cgmMinTv.text = (levels.minOrNull() ?: 0).toString()
    }

    private fun changeDateTextAccordingToChip(chipText: String){
        println("debug: in changeDateTextAccordingToChip $chipText")
        if (chipText == DAY){
            dateTextView.text = formatDate(selectedDate)

        }else if (chipText == WEEK){
            val c = Calendar.getInstance()
            c.time = Date()
            c.add(Calendar.DATE, -7)
            val startDayDate = c.time
            dateTextView.text = formatDate(startDayDate) + " - " + formatDate(Date())

        }else if (chipText == MONTH){
            dateTextView.text = formatMonth(selectedMonthAndYear)

        }else if (chipText == YEAR){
            dateTextView.text = formatYear((selectedYear))

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

    private fun formatMonth(date: Date): String{
        val c = Calendar.getInstance()
        c.time = date
        println("debug: c.get(Calendar.MONTH) "+ c.get(Calendar.MONTH).toString())
        val month = monthName[c.get(Calendar.MONTH)]
        val year = c.get(Calendar.YEAR).toString()
        return "$month $year"
    }

    private fun formatYear(date: Date): String{
        val c = Calendar.getInstance()
        c.time = date
        val year = c.get(Calendar.YEAR).toString()
        return "$year"
    }

    private fun setChipOnCheckedChangeListener() {
        cgmChipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != -1){
                checkedChip = group.findViewById(checkedId)
                checkedChipText = checkedChip.text.toString()
                println("debug: select chip $checkedChipText")

                // reshow the bar chart with new data
                if (checkedChipText == DAY){
                    renewDataSummary(cgmDataForDay)
                    drawAvgLimitLine(cgmBarChartCustom, cgmDataForDay)
                    showCgmBarChart(cgmDataForDay)
                    cgmBarChartCustom.invalidate()
                    cgmBarChartCustom.setVisibleXRange(16f,16f)
                    changeDateTextAccordingToChip(DAY)
                }else if (checkedChipText == WEEK){
                    renewDataSummary(cgmDataForWeek)
                    drawAvgLimitLine(cgmBarChartCustom, cgmDataForWeek)
                    showCgmBarChart(cgmDataForWeek)
                    cgmBarChartCustom.invalidate()
                    cgmBarChartCustom.setVisibleXRange(7f,7f)
                    changeDateTextAccordingToChip(WEEK)
                }else if (checkedChipText == MONTH){
                    renewDataSummary(cgmDataForMonth)
                    drawAvgLimitLine(cgmBarChartCustom, cgmDataForMonth)
                    showCgmBarChart(cgmDataForMonth)
                    cgmBarChartCustom.invalidate()
                    cgmBarChartCustom.setVisibleXRange(16f,16f)
                    changeDateTextAccordingToChip(MONTH)
                }else if (checkedChipText == YEAR){
                    renewDataSummary(cgmDataForYear)
                    drawAvgLimitLine(cgmBarChartCustom, cgmDataForYear)
                    showCgmBarChart(cgmDataForYear)
                    cgmBarChartCustom.invalidate()
                    cgmBarChartCustom.setVisibleXRange(6f,6f)
                    cgmBarChartCustom.setVisibleXRangeMaximum(7F)
                    changeDateTextAccordingToChip(YEAR)
                }

            }else{
                println("debug: select the same chip, check this chip again")
                checkedChip.isChecked = true
            }
        }
    }

    private fun showCgmBarChart(cgmData: List<AverageEgv>){
        val entries: ArrayList<BarEntry> = ArrayList()

        cgmData.forEachIndexed { index, s ->
            if(checkedChip.text == DAY){
                var nonNullValue = 0f
                if (s.value == null){
                    nonNullValue = 0f
                }else{
                    nonNullValue = s.value!!.toFloat()
                }
                entries.add(BarEntry((s.systemTime!!.hour + 2).toFloat(), nonNullValue))
            }
            else if(checkedChip.text == WEEK){
                entries.add(BarEntry((index + 1).toFloat(), s.value!!.toFloat()))
            }
            else if(checkedChip.text == MONTH){
                entries.add(BarEntry((index + 1).toFloat(), s.value!!.toFloat()))
            }
            else if(checkedChip.text == YEAR){
                entries.add(BarEntry((s.systemTime!!.monthValue + 1).toFloat(), s.value!!.toFloat()))
            }
        }

        val barDataSet = CustomBarDataSet(entries, "CGM DATA")

        val data = BarData(barDataSet)
        cgmBarChartCustom.data = data
        cgmBarChartCustom.invalidate()

        setOutlookOfCgmBarDataSet(barDataSet)
    }

    private fun setOutlookOfCgmBarDataSet(barDataSet: CustomBarDataSet){
        //Changing the color of the bar, which is mColor in CustomBarDataSet
        barDataSet.setColors(arrayListOf(
            ContextCompat.getColor(requireContext(), R.color.graph_low),
            ContextCompat.getColor(requireContext(), R.color.graph_medium),
            ContextCompat.getColor(requireContext(), R.color.graph_high)
        ))
        //Setting the size of the form in the legend
        barDataSet.setFormSize(15f)
        //showing the value of the bar, default true if not set
        barDataSet.setDrawValues(true)
        //setting the text size of the value of the bar
        barDataSet.setValueTextSize(12f)
        barDataSet.setDrawValues(false)
    }

    private fun setAppearanceOfCgmBarChart(barChartCustom: CustomRoundedBarChart){
        //hiding the grey background of the chart, default false if not set
        barChartCustom.setDrawGridBackground(false)
        //remove the bar shadow, default false if not set
        barChartCustom.setDrawBarShadow(false)
        //remove border of the chart, default false if not set
        barChartCustom.setDrawBorders(false)
        //remove the description label text located at the lower right corner
        barChartCustom.isClickable = false
        val description = Description()
        description.setEnabled(false)
        barChartCustom.setDescription(description)
        barChartCustom.setScaleEnabled(false)
        barChartCustom.moveViewToX(0F)
        cgmBarChartCustom.setVisibleXRange(16f,16f)

        val c = Calendar.getInstance()
        c.time = Date()
        println("debug: line 591 current hour " + c[Calendar.HOUR_OF_DAY].toString())
        var xPosition = c[Calendar.HOUR_OF_DAY]
        if(xPosition - 16 < 0){
            xPosition = 0
        }else{
            xPosition -= 16
        }
        barChartCustom.moveViewToX(xPosition.toFloat())

        //setting animation for y-axis, the bar will pop up from 0 to its value within the time we set
        barChartCustom.animateY(1000)
        //setting animation for x-axis, the bar will pop up separately within the time we set
        barChartCustom.animateX(1000)

        val xAxis: XAxis = barChartCustom.getXAxis()
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.setCenterAxisLabels(true)


        val yAxis: YAxis = barChartCustom.getAxisLeft()
        yAxis.setDrawAxisLine(true)
        yAxis.setDrawGridLines(false)
        yAxis.setDrawZeroLine(true)
        yAxis.axisMinimum = 0f
        yAxis.granularity = 1f

        val rightYAxis: YAxis = barChartCustom.getAxisRight()
        //hiding the right y-axis line, default true if not set
        rightYAxis.isEnabled = false

        val legend: Legend = barChartCustom.getLegend()
        //setting the shape of the legend form to line, default square shape
        legend.form = Legend.LegendForm.SQUARE
        //setting the text size of the legend
        legend.textSize = 11f
        //setting the alignment of legend toward the chart
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
        //setting the stacking direction of legend
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        //setting the location of legend outside the chart, default false if not set
        legend.setDrawInside(false)
        legend.setEnabled(false)

        val ll1 = LimitLine(100f, "Recommended level")
        ll1.lineWidth = 4f
        ll1.enableDashedLine(10f, 10f, 0f)
        ll1.labelPosition = LimitLabelPosition.RIGHT_TOP
        ll1.textSize = 15f
        ll1.textColor = Color.RED

        yAxis.addLimitLine(ll1)

    }

    private fun drawAvgLimitLine(barChartCustom: CustomRoundedBarChart, dataList: List<AverageEgv>){
        val yAxis: YAxis = barChartCustom.getAxisLeft()
        yAxis.removeAllLimitLines()

        val levels = arrayListOf<Int>()
        for (d in dataList){
            if(d.value != null){
                levels.add(d.value!!)
            }
        }

        val llRecommend = LimitLine(100f, "Recommended level")
        llRecommend.lineWidth = 2f
        llRecommend.enableDashedLine(10f, 10f, 0f)
        llRecommend.labelPosition = LimitLabelPosition.RIGHT_TOP
        llRecommend.textSize = 15f
        llRecommend.textColor = Color.RED

        val llAvg = LimitLine(levels.average().toFloat(), "Average level")
        llAvg.lineWidth = 2f
        llAvg.lineColor = Color.GRAY
        llAvg.enableDashedLine(10f, 10f, 0f)
        llAvg.labelPosition = LimitLabelPosition.LEFT_TOP
        llAvg.textSize = 15f
        llAvg.textColor = Color.GRAY

        yAxis.addLimitLine(llRecommend)
        yAxis.addLimitLine(llAvg)

    }
}