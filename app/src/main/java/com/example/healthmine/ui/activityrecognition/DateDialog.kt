package com.example.healthmine.ui.activityrecognition

import android.R
import android.app.DatePickerDialog
import android.app.Dialog
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.widget.DatePicker
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import java.util.*

class DateDialog: DialogFragment(), DatePickerDialog.OnDateSetListener {
    companion object{
        const val DIALOG_DATE_KEY = "date_dialog"
        const val SELECTDATE = 1
    }
    //Input the instant date and return it
    private lateinit var calendar: Calendar
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        lateinit var ret: Dialog
        calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)  //Get year set
        val month = calendar.get(Calendar.MONTH) //Get month set
        val day = calendar.get(Calendar.DAY_OF_MONTH) //Get day set
        val bundle = arguments
        val dialogId = bundle?.getInt(DateDialog.DIALOG_DATE_KEY)
        ret = DatePickerDialog(
            requireActivity(),
            R.style.Theme_DeviceDefault_Dialog,
            this,
            year,
            month,
            day

        )// Get the date user set on the dialog

        return ret

    }
    //When you hit OK, it make a toast. The content of the function cannot be empty, or the program will collapse
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, day: Int) {
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        val myFormat = "MMM dd yyyy" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        //dateInput = sdf.format(calendar.getTime())
        Toast.makeText(
            activity,
            "Date set"
            , Toast.LENGTH_SHORT
        ).show()
    }


}