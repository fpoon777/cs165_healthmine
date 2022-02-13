package com.example.healthmine.ui.cgm

import android.content.DialogInterface

import android.app.AlertDialog

import android.widget.NumberPicker

import android.view.LayoutInflater

import android.os.Bundle

import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.view.View
import androidx.fragment.app.DialogFragment
import com.example.healthmine.R
import java.util.*


class CustomMonthPicker(val date: Date) : DialogFragment() {
    private var listener: OnDateSetListener? = null
    fun setListener(listener: OnDateSetListener?) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        // Get the layout inflater
        val inflater: LayoutInflater = getLayoutInflater()

        val dialog: View = inflater.inflate(R.layout.custom_month_picker_dialog, null)
        val monthPicker = dialog.findViewById(R.id.picker_month) as NumberPicker
        val yearPicker = dialog.findViewById(R.id.picker_year) as NumberPicker


        val cal: Calendar = Calendar.getInstance()
        cal.time = date
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = cal.get(Calendar.MONTH) + 1

        val c = Calendar.getInstance()
        c.time = Date()
        yearPicker.minValue = 2018
        yearPicker.maxValue = c[Calendar.YEAR]
        yearPicker.value = cal.get(Calendar.YEAR)

        builder.setView(dialog) // Add action buttons
            .setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, id ->
                    listener!!.onDateSet(
                        null,
                        yearPicker.value,
                        monthPicker.value,
                        0
                    )
                })
            .setNegativeButton("CANCEL",
                DialogInterface.OnClickListener { dialog, id ->
                    this@CustomMonthPicker.getDialog()?.cancel()
                })
        return builder.create()
    }
//
//    companion object {
//        private const val MAX_YEAR = 2099
//    }
}