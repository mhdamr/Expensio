package com.example.fundcache

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.fundcache.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    lateinit var bottomNav : BottomNavigationView
    lateinit var toggle : ActionBarDrawerToggle
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var profileBox : LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get a reference to the FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val drawerLayout : DrawerLayout = findViewById(R.id.drawerLayout)
        val navView : NavigationView = findViewById(R.id.nav_view)

        // Find HeaderView of the NavigationView
        val headerView = navView.getHeaderView(0)

        // Find the Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)

        // Set the Toolbar as the support ActionBar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false);

        toggle = ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.open,R.string.close)

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.setDrawerIndicatorEnabled(true)

        navView.setNavigationItemSelectedListener {

            when(it.itemId) {

                R.id.drawer_bank_sync -> Toast.makeText(applicationContext,"Clicked Home", Toast.LENGTH_SHORT).show()
                R.id.drawer_log_out -> Toast.makeText(applicationContext,"Clicked Home", Toast.LENGTH_SHORT).show()
            }

            true
        }

        // Set up the navigation with the NavController and AppBarConfiguration
        val appBarConfiguration = AppBarConfiguration((setOf(
            R.id.homeFragment, R.id.walletsFragment,
            R.id.addWalletsFragment, R.id.settingsFragment)),drawerLayout)
        toolbar.setupWithNavController(navController, appBarConfiguration)



        bottomNav = findViewById(R.id.bottomNav) as BottomNavigationView
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    navController.navigate(R.id.action_homeFragment)
                    true
                }
                R.id.wallets -> {
                    navController.navigate(R.id.action_homeFragment_to_walletsFragment)
                    true
                }
                else -> {
                    true
                }
            }
        }

        bottomNav.background = null
        bottomNav.menu.getItem(1).isEnabled = false

        // Check if the user has a username in Firestore
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {

                        navController.navigate(R.id.homeFragment)

            // Check if the user has a name in Firestore
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userRef = db.collection("users").document(currentUser.uid)
                userRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        val name = documentSnapshot.getString("name")
                        if (name.isNullOrEmpty()) {
                            // Show a dialog to prompt the user to enter a name
                            showCreateProfileDialog()
                        }
                    }
                    .addOnFailureListener { e ->
                        // Show an error message
                        Log.e("HomeFragment", "Error retrieving user name: ${e.message}", e)
                    }
            }


        }

        userNameTextView = headerView.findViewById(R.id.user_name)
        userEmailTextView = headerView.findViewById(R.id.user_email)

        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name")
                    userNameTextView.text = name
                }
        }

        if (currentUser != null) {
            val email = currentUser.email
            userEmailTextView.text = email
        }


        profileBox = headerView.findViewById(R.id.profile_box)
        profileBox.setOnClickListener {
            showEditProfileDialog()
        }


    }

    private fun showCreateProfileDialog() {
        val createProfileDialogFragment = CreateProfileDialogFragment()
        createProfileDialogFragment.show(supportFragmentManager, "CreateProfileDialog")
    }

    private fun showEditProfileDialog() {
        val editProfileDialogFragment = EditProfileDialogFragment()
        editProfileDialogFragment.show(supportFragmentManager, "EditProfileDialog")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item)) {
            return true
        }
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
