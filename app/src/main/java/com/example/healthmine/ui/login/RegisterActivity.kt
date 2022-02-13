package com.example.healthmine.ui.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.healthmine.MainActivity
import com.example.healthmine.databinding.ActivityRegisterBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var registerButton:Button
    private lateinit var loginText:TextView
    private lateinit var nameInput:EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //hide action bar and support action bar
        actionBar?.hide();
        supportActionBar?.hide()

        emailInput = binding.registerEmailAddress
        passwordInput = binding.registerAccountPassword
        registerButton = binding.registerBtn
        loginText = binding.loginText       //goes back to login activity
        nameInput = binding.registerAccountName

        //follow the tutorial from https://www.youtube.com/watch?v=8I5gCLaS25w&t=1054s
        registerButton.setOnClickListener {
            registerUser()
        }

        loginText.setOnClickListener{
            redirectToLogin()
        }
    }

    private fun registerUser(){
        when{
            TextUtils.isEmpty(nameInput.text.toString().trim{ it <= ' '})->{
                Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show()
            }

            TextUtils.isEmpty(emailInput.text.toString().trim{ it <= ' '})->{
                Toast.makeText(this, "Please enter email address", Toast.LENGTH_SHORT).show()
            }

            TextUtils.isEmpty(passwordInput.text.toString().trim{ it <= ' '})->{
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
            }

            else -> {
                val emailAddress = emailInput.text.toString().trim{ it <= ' '}
                val password = passwordInput.text.toString().trim{ it <= ' '}
                val name = nameInput.text.toString().trim{ it <= ' '}

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailAddress, password)
                    .addOnCompleteListener {
                        if (it.isSuccessful){
                            val firebaseUser:FirebaseUser = it.result!!.user!!

                            Toast.makeText(this, "Registered Successfully!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this,MainActivity::class.java)
                            // so that a new activity is created
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            val bundle = Bundle()
                            bundle.putString("user_name", name)
                            bundle.putString("email_id", firebaseUser.email)
                            intent.putExtras(bundle)
                            startActivity(intent)
                            finish()
                        }
                        else{
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    // go to login activity
    private fun redirectToLogin(){
        val intent = Intent(this,LoginActivity::class.java)
        // so that a new activity is created
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}