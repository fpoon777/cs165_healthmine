package com.example.healthmine.ui.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.healthmine.R
import com.example.healthmine.databinding.FragmentSettingBinding
import com.example.healthmine.ui.firebase.FirebaseUtil
import com.example.healthmine.ui.login.LoginActivity
import com.example.healthmine.utils.DexcomUtil
import com.example.healthmine.utils.DexcomUtil.writeAuthState
import com.example.healthmine.utils.UnauthorizedException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers.IO
import net.openid.appauth.*
import com.ebanx.swipebtn.SwipeButton
import android.widget.Toast

import com.example.healthmine.database.*
import kotlinx.coroutines.*


class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var dexcomSwitch: Switch
    private lateinit var activitySwitch: Switch
    private lateinit var sleepSwitch: Switch

    private lateinit var authState: AuthState

    private lateinit var serviceConfig: AuthorizationServiceConfiguration
    private lateinit var authRequestBuilder: AuthorizationRequest.Builder
    private lateinit var authRequest: AuthorizationRequest
    private lateinit var authService: AuthorizationService

    private lateinit var contentView: ScrollView
    private lateinit var progressBarView: LinearLayout

    //logout and change passwords
    private lateinit var changePasswordBtn:Button
    private lateinit var logoutBtn:Button
    private lateinit var editNameTextView:TextView
    private lateinit var nameTextView:TextView
    private lateinit var emailTextView:TextView
//    private lateinit var syncToBtn:SwipeButton
//    private lateinit var syncFromBtn:SwipeButton

//    database
    private lateinit var database: HealthmineDatabase
    private lateinit var databaseDao: SleepSegmentEventDao
    private lateinit var databaseClassifyDao: SleepClassifyEventDao
    private lateinit var repository: SleepRepository
    private lateinit var actdatabaseDao: ActivityRecognitionDao
    private lateinit var actrepository: ActivityRecognitionRepository


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        changePasswordBtn = binding.settingPasswordBtn
        logoutBtn = binding.settingLogoutBtn
        editNameTextView = binding.settingEditInfo
        nameTextView = binding.settingNameView
//        syncToBtn = binding.settingSyncToBtn
//        syncFromBtn= binding.settingSyncFromBtn
        emailTextView = binding.settingEmailView

        readNameAndEmail()

        changePasswordBtn.setOnClickListener {
            openDialog()
        }

        editNameTextView.setOnClickListener {
            openNameDialog()
        }

        logoutBtn.setOnClickListener {
            logout()
        }

        //sync data stored in the local database to firebase
//        syncToBtn.setOnStateChangeListener {
//            if (syncToBtn.isActive){
//                CoroutineScope(IO).launch {
//                    FirebaseUtil.uploadDataToFirebase(requireActivity())
//                    withContext(Dispatchers.Main){
//                        Toast.makeText(requireContext(), "Sync to Firebase Finished!", Toast.LENGTH_SHORT).show()
//                        syncToBtn.toggleState()
//                    }
//                }
//            }
//        }
//
//        //sync data from firebase for this user
//        syncFromBtn.setOnStateChangeListener {
//            if (syncFromBtn.isActive){
//                CoroutineScope(IO).launch {
//                    FirebaseUtil.readDataFromFirebase(requireActivity())
//                    withContext(Dispatchers.Main){
//                        Toast.makeText(requireContext(), "Sync From Firebase Finished!", Toast.LENGTH_SHORT).show()
//                        syncFromBtn.toggleState()
//                    }
//                }
//            }
//        }

        contentView = root.findViewById(R.id.content_view)
        progressBarView = root.findViewById(R.id.progress_bar_view)

//        set OAuth2 auth configs
        authService = AuthorizationService(requireContext())

        serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("https://sandbox-api.dexcom.com/v2/oauth2/login"),  // authorization endpoint
            Uri.parse("https://sandbox-api.dexcom.com/v2/oauth2/token")
        ) // token endpoint

        authState = AuthState(serviceConfig)

        authRequestBuilder = AuthorizationRequest.Builder(
            serviceConfig,  // the authorization service configuration
            "oFgTBz6DxFaKe3U2DUsLTBcxTz9mcJlb",  // the client ID, typically pre-registered and static
            ResponseTypeValues.CODE,  // the response_type value: we want a code
            Uri.parse("healthmine://callback")
        ) // the redirect URI to which the auth response is sent

        authRequest = authRequestBuilder.build()

        //Set activity recognition switch
        activitySwitch = root.findViewById(R.id.switch1)
        activitySwitch.isChecked = true
        activitySwitch.setOnCheckedChangeListener{ buttonView, isChecked ->
            val giveHint = ActivityHintDialog(0)
            val bundle = Bundle()
            giveHint.arguments = bundle
            giveHint.show(childFragmentManager, "duration dialog")
            activitySwitch.isChecked = true
        }

        //Set sleep recognition switch
        sleepSwitch = root.findViewById(R.id.switch4)
        sleepSwitch.isChecked = true
        sleepSwitch.setOnCheckedChangeListener{ buttonView, isChecked ->
            val giveHint = ActivityHintDialog(1)
            val bundle = Bundle()
            giveHint.arguments = bundle
            giveHint.show(childFragmentManager, "duration dialog")
            sleepSwitch.isChecked = true
        }

//        set dexcom switch listener
        dexcomSwitch = root.findViewById(R.id.switch6)
        dexcomSwitch.isChecked = DexcomUtil.readAuthState(requireContext()) != null
        dexcomSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                println("debug: checked")
                val authIntent = authService.getAuthorizationRequestIntent(authRequest)
                authIntentLauncher.launch(authIntent)
            } else {
                println("debug: unchecked")
                DexcomUtil.deleteAuthState(requireContext())
            }
        }

        return root
    }

    //read email and name from firebase and update in the ui
    private fun readNameAndEmail(){
        FirebaseUtil.readNameFromFirebase(nameTextView)
        FirebaseUtil.readEmailFromFirebase(emailTextView)
    }

    //open reset password dialog
    private fun openDialog(){
        val resetPasswordDialog=ResetPasswordDialog()
        resetPasswordDialog.show(childFragmentManager,"reset dialog")
    }

    //open reset name dialog
    private fun openNameDialog(){
        val resetNameDialog=ResetNameDialog()
        resetNameDialog.show(childFragmentManager, "reset name dialog")
    }

    //logout the user
    private fun logout(){
        database = HealthmineDatabase.getInstance(requireActivity())
        databaseDao = database.sleepSegmentEventDao
        databaseClassifyDao = database.sleepClassifyEventDao
        repository = SleepRepository(databaseDao, databaseClassifyDao)
        actdatabaseDao = database.activityDatabaseDao
        actrepository = ActivityRecognitionRepository(actdatabaseDao)

        CoroutineScope(IO).launch {
            repository.deleteAllSegments()
            actrepository.deleteAll()
        }

        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }

    private suspend fun updateViewVisibilityOnMainThread(contentView: View, progressBarView: View) {
        withContext(Dispatchers.Main){
            contentView.isVisible = true
            progressBarView.isVisible = false
        }
    }

    private suspend fun showToastOnMainThread(text: String) {
        withContext(Dispatchers.Main){
            Toast.makeText(requireContext(), text.toString(), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//    auth intent launcher
    private var authIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val resp = AuthorizationResponse.fromIntent(it.data!!)
        val ex = AuthorizationException.fromIntent(it.data)

        authState.update(resp, ex)

        if (resp != null) {

//                Exchanging the authorization code
            val clientAuth: ClientAuthentication = ClientSecretPost("XPGqOWsw8OhPPHfL")

            authService.performTokenRequest(
                resp.createTokenExchangeRequest(),
                clientAuth,
                AuthorizationService.TokenResponseCallback { resp, ex ->

                    authState.update(resp, ex)

                    if (resp != null) {
                        // exchange succeeded
                            println("debug: $resp")
                        writeAuthState(requireContext(), authState)
//                        get data
                        contentView.isVisible = false
                        progressBarView.isVisible = true
                        CoroutineScope(IO).launch {
                            try {
                                DexcomUtil.syncEgvs(requireContext())
                            } catch (e: UnauthorizedException) {
                                DexcomUtil.deleteAuthState(requireContext())
                                showToastOnMainThread(e.message)
                            }
                            updateViewVisibilityOnMainThread(contentView, progressBarView)
                        }
                    } else {
                        // authorization failed, check ex for more details
                        Toast.makeText(requireContext(), ex.toString(), Toast.LENGTH_LONG).show()
                        dexcomSwitch.isChecked = false
                    }
                })
        } else {
            Toast.makeText(requireContext(), ex.toString(), Toast.LENGTH_LONG).show()
            dexcomSwitch.isChecked = false
        }
    }
}