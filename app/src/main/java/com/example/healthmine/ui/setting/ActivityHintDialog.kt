package com.example.healthmine.ui.setting

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.healthmine.R

class ActivityHintDialog(private val tag: Int): DialogFragment(), DialogInterface.OnClickListener {
    private lateinit var hint: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        lateinit var ret: Dialog
        val builder = AlertDialog.Builder(requireActivity())
        val view: View = requireActivity().layoutInflater.inflate(R.layout.dialog_activity_hint, null)
        hint = view.findViewById(R.id.hintText)
        hint.text = if (tag == 0) {
            "Please keep the switch on to continue monitoring your activity mode"
        } else {
            "Please keep the switch on to continue monitoring your sleep"
        }

        builder.setView(view)
        builder.setPositiveButton("Okay", this)
        ret = builder.create()

        return ret
    }

    override fun onClick(dialog: DialogInterface, item: Int) {
        if (item == DialogInterface.BUTTON_POSITIVE) {
            try {

            }
            catch (e:Exception){
                Toast.makeText(activity, "Unknown error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}