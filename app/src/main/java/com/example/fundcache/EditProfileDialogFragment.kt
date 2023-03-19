package com.example.fundcache

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileDialogFragment : DialogFragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // Get a reference to the FirebaseAuth instance
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_edit_profile)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val saveChanges = dialog.findViewById<Button>(R.id.save_changes_btn)
        val nameEditText = dialog.findViewById<EditText>(R.id.edit_name)
        val emailEditText = dialog.findViewById<EditText>(R.id.edit_email)
        val changeEmailButton = dialog.findViewById<Button>(R.id.change_email_btn)
        val verifyEmailButton = dialog.findViewById<Button>(R.id.verify_email_btn)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)


        val headerView = (requireActivity() as MainActivity).findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)

        userNameTextView = headerView.findViewById(R.id.user_name)
        userEmailTextView = headerView.findViewById(R.id.user_email)

        // Set the current user name and email to the EditText fields
        nameEditText.setText(userNameTextView.text)
        emailEditText.setText(userEmailTextView.text)


        // Set up the cancel button
        cancelButton.setOnClickListener {
            dismiss()
        }

        // Disable the verify email button
        verifyEmailButton.isEnabled = false

        // Set an onClickListener on the "Change Email" button
        changeEmailButton.setOnClickListener {
            val newEmail = emailEditText.text.toString().trim()
            val currentUser = auth.currentUser

            // Check if the new email is valid
            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                emailEditText.error = "Invalid email"
                return@setOnClickListener
            }

            // Check if the new email is different from the current email
            if (currentUser?.email == newEmail) {
                emailEditText.error = "Email is already in use"
                return@setOnClickListener
            }

            // Disable the Save button
            saveChanges.isEnabled = false

            // Send verification email to new email
            auth.currentUser?.updateEmail(newEmail)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Verification email sent to $newEmail", Toast.LENGTH_SHORT).show()

                            // Enable the verify email button
                            verifyEmailButton.isEnabled = true

                            verifyEmailButton.setOnClickListener {
                                val user = auth.currentUser
                                user?.reload()?.addOnCompleteListener { reloadTask ->
                                    if (reloadTask.isSuccessful) {
                                        if (user.isEmailVerified) {
                                            db.collection("users").document(user.uid)
                                                .update("email", newEmail)
                                                .addOnSuccessListener {
                                                    userEmailTextView.text = newEmail
                                                    Toast.makeText(requireContext(), "Email updated", Toast.LENGTH_SHORT).show()
                                                    // Enable the Save button
                                                    saveChanges.isEnabled = true
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(requireContext(), "Failed to update email", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            Toast.makeText(requireContext(), "Email not verified", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to reload user", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                        } else {
                            Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
        }


        // Set an onClickListener on the "Save Changes" button
        saveChanges.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("users").document(currentUser.uid)
                    .update("name", name)
                    .addOnSuccessListener {
                        userNameTextView.text = name
                        dismiss()
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        return dialog
    }






}