package com.example.healthmine

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.healthmine.databinding.ActivityMainBinding
import com.example.healthmine.ui.firebase.FirebaseUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var userName:String?=""
    private var email:String?=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val bundle = intent.extras
        userName = bundle?.getString("user_name", "")
        email = bundle?.getString("email_id", "")

        //comes from register activity
        if (userName != ""){
            FirebaseUtil.firstTimeToDatabase(this,userName!!,email!!)
            registerUserSp(email!!)     //write first time user email address to sp
        }
        //from login activity
        if (userName == "" && email !=""){
            syncUser(email!!, this)
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_activity, R.id.nav_cgm, R.id.nav_sleep, R.id.nav_self_report,
                R.id.nav_setting, R.id.nav_feedback
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    //write the useremail of the first time user to the sp
    private fun registerUserSp(email:String){
        val sp: SharedPreferences = this.getSharedPreferences("login", Context.MODE_PRIVATE)
        val editor:SharedPreferences.Editor =  sp.edit()
        editor.putString("prev_email", email)
        editor.commit()
    }

    //check if the current login user is the same as the previous logged in user
    private fun syncUser(email:String, activity: Activity){
        val sp: SharedPreferences = this.getSharedPreferences("login", Context.MODE_PRIVATE)
        val prevEmail = sp.getString("prev_email", "")
        val editor:SharedPreferences.Editor =  sp.edit()

        if (prevEmail != email){
            editor.putString("prev_email", email)
            editor.commit()
            CoroutineScope(Dispatchers.IO).launch {
                FirebaseUtil.readDataFromFirebase(activity)
                withContext(Dispatchers.Main){
                    Toast.makeText(activity, "Sync your data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}