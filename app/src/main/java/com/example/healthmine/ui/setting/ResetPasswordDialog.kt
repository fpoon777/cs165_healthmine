package com.example.healthmine.ui.setting

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.helper.widget.MotionEffect
import androidx.fragment.app.DialogFragment
import com.example.healthmine.R
import com.example.healthmine.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sign

class ResetPasswordDialog: DialogFragment(), DialogInterface.OnClickListener {
    private lateinit var resetText1:EditText
    private lateinit var resetText2:EditText
    private var isSuccessful:Boolean = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        lateinit var ret: Dialog
        val builder = AlertDialog.Builder(requireActivity())
        val view: View = requireActivity().layoutInflater.inflate(R.layout.fragment_reset_password_dialog, null)
        resetText1 = view.findViewById(R.id.reset_password_1)
        resetText2 = view.findViewById(R.id.reset_password_2)

        builder.setView(view)
        builder.setPositiveButton("Reset", this)
        builder.setNegativeButton("Cancel", this)
        ret = builder.create()

        return ret
    }

    override fun onClick(dialog: DialogInterface, item: Int) {
        if (item == DialogInterface.BUTTON_POSITIVE) {
            try {
                if (resetText1.text.toString() == resetText2.text.toString()){
                    if (resetText1.text.toString().length >= 6){
                        changePassword(resetText1.text.toString())
                        signOut()
                    }
                    else{
                        Toast.makeText(activity, "Invalid password", Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    Toast.makeText(activity, "Passwords don't match", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e:Exception){
                Toast.makeText(activity, "Please enter both fields", Toast.LENGTH_SHORT).show()
            }
        } else if (item == DialogInterface.BUTTON_NEGATIVE) {}
    }


    private fun changePassword(newPassword:String){
        val user = FirebaseAuth.getInstance().currentUser

        user!!.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isSuccessful = true
                }
            }
    }

    private fun signOut(){
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }
}