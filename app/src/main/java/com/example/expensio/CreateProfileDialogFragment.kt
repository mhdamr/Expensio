package com.example.expensio

import android.app.Dialog
import android.content.ContentValues.TAG
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CreateProfileDialogFragment : DialogFragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userNameTextView: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // Get a reference to the FirebaseAuth and Firebase Firestore instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_create_profile)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnSave = dialog.findViewById<Button>(R.id.saveName)
        val txtName = dialog.findViewById<EditText>(R.id.nameEditText)

        val headerView = (requireActivity() as MainActivity).findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)

        userNameTextView = headerView.findViewById(R.id.user_name)

        btnSave.setOnClickListener {
            val name = txtName.text.toString().trim()
            if (name.length < 1 || !name.matches(Regex("^(?=.*[a-zA-Z0-9]).+\$"))) {
                txtName.error = "Please enter a valid name"
                return@setOnClickListener
            }
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val options = SetOptions.merge()
                val userMap = mutableMapOf<String, Any>("name" to name)
                db.collection("users").document(currentUser.uid)
                    .set(userMap, options)
                    .addOnSuccessListener {
                        userNameTextView.text = name
                        dismiss()
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Error saving name: ${e.message}", e)
                    }
            }
        }
        return dialog
    }
}