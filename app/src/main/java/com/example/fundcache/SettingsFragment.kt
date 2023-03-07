package com.example.fundcache

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import com.example.fundcache.R
import android.content.Intent

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)


        // Get a reference to the FirebaseAuth and FirebaseFirestore instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get a reference to the SharedPreferences instance
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Get a reference to the "Name" preference and set its summary to the user's name
        val namePreference = findPreference<Preference>("account_name")
        if (namePreference != null) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("users").document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val name = document.getString("name")
                        namePreference.summary = name
                    }
                    .addOnFailureListener { e ->
                        // Show an error message
                        Toast.makeText(context, "Error retrieving user name: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Get a reference to the "Email" preference and set its summary to the user's email
        val emailPreference = findPreference<Preference>("account_email")
        if (emailPreference != null) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val email = currentUser.email
                emailPreference.summary = email
            }
        }

        // Get a reference to the "Log Out" preference and set a click listener on it
        val logoutPreference = findPreference<Preference>("account_logout")
        if (logoutPreference != null) {
            logoutPreference.setOnPreferenceClickListener {
                // Log the user out
                auth.signOut()
                // Redirect the user to the login screen
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                true
            }
        }
    }
}