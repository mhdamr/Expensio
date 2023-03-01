package com.example.fundcache

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.fundcache.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.widget.Toast
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get a reference to the FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        // Set up the ActionBar
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up the ActionBar
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Check if the user has a username in Firestore
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = db.collection("users").document(currentUser.uid)
            userRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    val name = documentSnapshot.getString("name")
                    if (name.isNullOrEmpty() || name.isEmpty() || name.length < 1 || !name.matches(Regex("^(?=.*[a-zA-Z0-9]).+\$"))) {
                        // User does not have a valid username, navigate to ChooseNameFragment
                        Log.d("Navigation", "Navigating to ChooseNameFragment")
                        navController.navigate(R.id.action_homeFragment_to_chooseNameFragment)
                    }
                }
                .addOnFailureListener { e ->
                    // Show an error message
                    Toast.makeText(this, "Error retrieving user name: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Navigate to the settings page
                navController.navigate(R.id.settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}