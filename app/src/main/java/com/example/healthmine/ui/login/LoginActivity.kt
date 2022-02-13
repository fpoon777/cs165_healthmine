package com.example.healthmine.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.healthmine.MainActivity
import com.example.healthmine.databinding.ActivityLoginBinding
import com.example.healthmine.utils.FirebaseDatabaseUtil
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var registerButton:Button
    private lateinit var registerText:TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        ImageView.setAdjustViewBounds(true)

        //hide action bar and support action bar
        actionBar?.hide();
        supportActionBar?.hide()

        emailInput = binding.emailAddress
        passwordInput = binding.accountPassword
        registerButton = binding.loginBtn
        registerText = binding.registerText       //goes back to register activity

        //follow the tutorial from https://www.youtube.com/watch?v=8I5gCLaS25w&t=1054s
        registerButton.setOnClickListener {
            loginUser()
        }

        registerText.setOnClickListener {
            redirectToRegister()
        }
    }

    private fun loginUser(){
        when{
            TextUtils.isEmpty(emailInput.text.toString().trim{ it <= ' '})->{
                Toast.makeText(this, "Please enter email address", Toast.LENGTH_SHORT).show()
            }

            TextUtils.isEmpty(passwordInput.text.toString().trim{ it <= ' '})->{
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
            }

            else -> {
                val emailAddress = emailInput.text.toString().trim{ it <= ' '}
                val password = passwordInput.text.toString().trim{ it <= ' '}

                FirebaseAuth.getInstance().signInWithEmailAndPassword(emailAddress, password)
                    .addOnCompleteListener {
                        if (it.isSuccessful){
                            val firebaseUser:FirebaseUser = it.result!!.user!!

//                            download sleep data
                            FirebaseDatabaseUtil.downloadSleepData(this)
                            FirebaseDatabaseUtil.downloadActivityTimestampData(this)

                            Toast.makeText(this, "Login Successfully!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this,MainActivity::class.java)
                            // so that a new activity is created
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            val bundle = Bundle()
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
    private fun redirectToRegister(){
        val intent = Intent(this,RegisterActivity::class.java)
        // so that a new activity is created
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}