package com.example.healthmine.ui.splash

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.healthmine.R
import com.example.healthmine.MainActivity

import android.content.Intent

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}