package com.example.expensio.Auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.expensio.MainActivity
import com.example.expensio.R
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var saveChangesButton: Button
    private var newEmailVerified: Boolean = true
    private var changeEmailClicked: Boolean = false
    private lateinit var currentEmail: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        // Hide the bottom app bar
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.GONE

        // Get a reference to the FirebaseAuth instance
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        saveChangesButton = rootView.findViewById<Button>(R.id.save_changes_btn)
        val nameEditText = rootView.findViewById<EditText>(R.id.edit_name)
        val emailEditText = rootView.findViewById<EditText>(R.id.edit_email)
        val changeEmailButton = rootView.findViewById<Button>(R.id.change_email_btn)


        val headerView = (requireActivity() as MainActivity).findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)

        userNameTextView = headerView.findViewById(R.id.user_name)
        userEmailTextView = headerView.findViewById(R.id.user_email)

        // Set the current user name and email to the EditText fields
        nameEditText.setText(userNameTextView.text)
        emailEditText.setText(userEmailTextView.text)

        // Check if the user's email is verified and enable/disable buttons accordingly
        auth.currentUser?.reload()?.addOnCompleteListener {
            if (it.isSuccessful) {
                val isEmailVerified = auth.currentUser?.isEmailVerified ?: false
                saveChangesButton.isEnabled = isEmailVerified
                changeEmailButton.isEnabled = !isEmailVerified
            }
        }

        // Save the current email when the fragment is created
        currentEmail = userEmailTextView.text.toString()

        // Set up the "Change Email" button click listener
        changeEmailButton.setOnClickListener {
            val newEmail = emailEditText.text.toString().trim()

            // Check if the new email is valid
            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                emailEditText.error = "Invalid email"
                return@setOnClickListener
            }

            // Check if the new email is different from the current email
            if (auth.currentUser?.email == newEmail) {
                emailEditText.error = "Email is already in use"
                return@setOnClickListener
            }

            // Set newEmailVerified to false and changeEmailClicked to true when sending a verification email
            newEmailVerified = false
            changeEmailClicked = true
            sendVerificationEmail(newEmail, emailEditText)
        }

        // Add a listener to track changes in the emailEditText
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Enable the "Change Email" button if the emailEditText is not empty
                changeEmailButton.isEnabled = !emailEditText.text.isNullOrEmpty()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Set up the "Save Changes" button click listener
        saveChangesButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val newEmail = emailEditText.text.toString().trim()


            // Check if the name and email are the same as the current values
            if (userNameTextView.text == name && userEmailTextView.text == newEmail) {
                Toast.makeText(requireContext(), "No changes were made.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the name is different from the current value
            if (userNameTextView.text != name) {
                updateUserProfile(name, newEmail)
                Toast.makeText(requireContext(), "Display name updated.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the email has changed and changeEmailButton has not been clicked
            if (currentEmail != newEmail && !changeEmailClicked) {
                Toast.makeText(requireContext(), "Enter a valid new email and press the 'Change Email' button.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the email has changed, changeEmailButton has been clicked and newEmailVerified is false
            if (currentEmail != newEmail && changeEmailClicked && !newEmailVerified) {
                Toast.makeText(requireContext(), "Please check your inbox and verify your email.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the user's new email is verified before saving changes
            if (newEmailVerified) {
                updateUserProfile(name, newEmail)  // Include the newEmail when calling updateUserProfile
            } else {
                Toast.makeText(requireContext(), "New email is not verified. Please verify your new email.", Toast.LENGTH_SHORT).show()
            }
        }

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show the bottom app bar when leaving the fragment
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.VISIBLE
    }

    private fun sendVerificationEmail(newEmail: String, emailEditText: EditText) {
        val user = auth.currentUser

        // Update the user's email
        user?.updateEmail(newEmail)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Send verification email after updating the email in Firebase Auth
                user.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                    if (verificationTask.isSuccessful) {
                        Toast.makeText(requireContext(), "Verification email sent to $newEmail", Toast.LENGTH_SHORT).show()

                        // Update the currentEmail and the email displayed in the UI
                        currentEmail = newEmail
                        userEmailTextView.text = newEmail
                        emailEditText.setText(newEmail)
                    } else {
                        Toast.makeText(requireContext(), "Failed to send verification email", Toast.LENGTH_SHORT).show()
                        newEmailVerified = true
                        changeEmailClicked = false
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Failed to change email", Toast.LENGTH_SHORT).show()
                newEmailVerified = true
                changeEmailClicked = false
            }
        }
    }



    private fun updateUserProfile(name: String, newEmail: String) {
        val currentUser = auth.currentUser
        currentUser?.let {
            db.collection("users").document(currentUser.uid)
                .update("name", name)
                .addOnSuccessListener {
                    userNameTextView.text = name
                    // If the email has changed and is verified, update the email in the UI
                    if (newEmail != currentEmail && newEmailVerified) {
                        userEmailTextView.text = newEmail
                        Toast.makeText(requireContext(), "Email address updated.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                        }
                    saveChangesButton.isEnabled = true
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                    saveChangesButton.isEnabled = true
                }
        }
    }

}