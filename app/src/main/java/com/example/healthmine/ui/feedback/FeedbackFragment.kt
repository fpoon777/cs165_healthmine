package com.example.healthmine.ui.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.healthmine.databinding.FragmentFeedbackBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FeedbackFragment: Fragment() {
    private var _binding: FragmentFeedbackBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var message: EditText
    private lateinit var sendButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        val root: View = binding.root

        name = binding.name
        email = binding.email
        message = binding.message
        sendButton = binding.sendButton

        sendButton.setOnClickListener {
            if (message.text.isNullOrEmpty()) {
                Toast
                    .makeText(requireContext(), "Message can not be empty!", Toast.LENGTH_SHORT)
                    .show()
//                FirebaseDatabaseUtil.read<Feedback>("sleep")
//                val test = SleepSegmentEventEntity("1", 1, 1, 1)
//                val testName = SleepSegmentEventEntity::startTimeMillis.name
//                println("debugfirebase: $testName")
            } else {
                val feedback = Feedback(message.text.toString())
                if (!name.text.isNullOrEmpty()) {
                    feedback.name = name.text.toString()
                }
                if (!email.text.isNullOrEmpty()) {
                    feedback.email = email.text.toString()
                }
                // Write a message to the database
                val database = Firebase.database
                val myRef = FirebaseAuth.getInstance().currentUser?.let { user ->
                    database.getReference(user.uid).child("feedback")
                }

                myRef?.push()?.setValue(feedback)

                name.text = null
                email.text = null
                message.text = null

                Toast
                    .makeText(requireContext(), "Feedback Sent!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        return root
    }
}