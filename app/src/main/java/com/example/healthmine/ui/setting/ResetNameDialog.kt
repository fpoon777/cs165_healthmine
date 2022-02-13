package com.example.healthmine.ui.setting

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.healthmine.R
import com.example.healthmine.ui.firebase.FirebaseUtil
import com.example.healthmine.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResetNameDialog: DialogFragment(), DialogInterface.OnClickListener {
    private lateinit var resetText1: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        lateinit var ret: Dialog
        val builder = AlertDialog.Builder(requireActivity())
        val view: View = requireActivity().layoutInflater.inflate(R.layout.fragment_reset_name_dialog, null)
        resetText1 = view.findViewById(R.id.reset_name)

        builder.setView(view)
        builder.setPositiveButton("Reset", this)
        builder.setNegativeButton("Cancel", this)
        ret = builder.create()

        return ret
    }

    override fun onClick(dialog: DialogInterface, item: Int)  {
        if (item == DialogInterface.BUTTON_POSITIVE) {
            try {
                if (resetText1.text.toString() != ""){
                    changeName(resetText1.text.toString(),resetText1)
                    signOut()
                }
                else{
                    Toast.makeText(activity, "No name is entered", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e:Exception){
                Toast.makeText(activity, "Unknown error", Toast.LENGTH_SHORT).show()
            }
        } else if (item == DialogInterface.BUTTON_NEGATIVE) {}
    }

    private fun changeName(newName:String, textView:TextView){
        FirebaseUtil.updateNameToDatabase(newName)
    }

    private fun signOut(){
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }
}