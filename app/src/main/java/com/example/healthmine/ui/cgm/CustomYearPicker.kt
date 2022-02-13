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


class CustomYearPicker(val date: Date) : DialogFragment() {
    private var listener: OnDateSetListener? = null
    fun setListener(listener: OnDateSetListener?) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        // Get the layout inflater
        val inflater: LayoutInflater = getLayoutInflater()

        val dialog: View = inflater.inflate(R.layout.custom_year_picker_dialog, null)
        val yearPicker = dialog.findViewById(R.id.picker_year_2) as NumberPicker

        val cal: Calendar = Calendar.getInstance()
        cal.time = date

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
                        0,
                        0
                    )
                })
            .setNegativeButton("CANCEL",
                DialogInterface.OnClickListener { dialog, id ->
                    this@CustomYearPicker.getDialog()?.cancel()
                })
        return builder.create()
    }
//
//    companion object {
//        private const val MAX_YEAR = 2099
//    }
}