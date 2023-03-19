package com.example.fundcache

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
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
    lateinit var toggle : ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get a reference to the FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val prefs = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val hasLoggedInBefore = prefs.getBoolean("hasLoggedInBefore", false)
        val currentUser = auth.currentUser

        // Check if the user is already logged in
        if (currentUser != null && currentUser.isEmailVerified) {
            if (hasLoggedInBefore) {
                // Navigate to the HomeFragment
                navController.navigate(R.id.homeFragment)
            }
        } else {
            // Navigate to the LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Find the Drawer Layout
        val drawerLayout : DrawerLayout = findViewById(R.id.drawerLayout)

        // Find the Navigation View
        val navView : NavigationView = findViewById(R.id.nav_view)

        // Find HeaderView of the Navigation View
        val headerView = navView.getHeaderView(0)

        // Find the Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)

        // Set the Toolbar as the support Action Bar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false);

        // Set the function of the hamburger menu to open/close the Navigation Drawer
        toggle = ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.open,R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.setDrawerIndicatorEnabled(true)


        // Set the items in the Navigation Drawer
        navView.setNavigationItemSelectedListener {

            when(it.itemId) {

                R.id.drawer_bank_sync ->
                    Toast.makeText(applicationContext,"Clicked Home", Toast.LENGTH_SHORT).show()
                R.id.drawer_log_out -> {
                    // Handle Log Out menu item click
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }

            true
        }

        // Add a dark/light mode switch to the drawer
        val switchDarkMode = navView.menu.findItem(R.id.drawer_dark_mode_switch).actionView as Switch

        // Check if the app is currently in night mode and update the switch and text
        if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            switchDarkMode.isChecked = true
            navView.menu.findItem(R.id.drawer_dark_mode_switch).setTitle(R.string.drawer_dark_mode_title)
            navView.menu.findItem(R.id.drawer_dark_mode_switch).setIcon(R.drawable.icon_dark_mode)
        } else {
            switchDarkMode.isChecked = false
            navView.menu.findItem(R.id.drawer_dark_mode_switch).setTitle(R.string.drawer_light_mode_title)
            navView.menu.findItem(R.id.drawer_dark_mode_switch).setIcon(R.drawable.icon_light_mode)
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Toggle the app theme based on the switch state
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                navView.menu.findItem(R.id.drawer_dark_mode_switch).setTitle(R.string.drawer_dark_mode_title)
                navView.menu.findItem(R.id.drawer_dark_mode_switch).setIcon(R.drawable.icon_dark_mode)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                navView.menu.findItem(R.id.drawer_dark_mode_switch).setTitle(R.string.drawer_light_mode_title)
                navView.menu.findItem(R.id.drawer_dark_mode_switch).setIcon(R.drawable.icon_light_mode)
            }
        }

        // Set up the navigation with the NavController and AppBarConfiguration
        val appBarConfiguration = AppBarConfiguration((setOf(
            R.id.homeFragment, R.id.walletsFragment,
            R.id.addWalletsFragment, R.id.settingsFragment)),drawerLayout)
        toolbar.setupWithNavController(navController, appBarConfiguration)


        // Find the Bottom Navigation View
        val bottomNav : BottomNavigationView = findViewById(R.id.bottomNav)

        // Set the items in the Bottom Navigation View
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.wallets -> {
                    navController.navigate(R.id.walletsFragment)
                    true
                }
                else -> {
                    true
                }
            }
        }
        bottomNav.background = null
        bottomNav.menu.getItem(1).isEnabled = false


        // Show the Create Profile Dialog if the user does not have a display name
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


        // Show the Edit Profile Dialog
        val userNameTextView : TextView = headerView.findViewById(R.id.user_name)
        val userEmailTextView : TextView = headerView.findViewById(R.id.user_email)

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

        val profileBox : LinearLayout = headerView.findViewById(R.id.profile_box)
        profileBox.setOnClickListener {
            showEditProfileDialog()
        }


    }

    // Activates the function which shows CreateProfileDialogFragment
    private fun showCreateProfileDialog() {
        val createProfileDialogFragment = CreateProfileDialogFragment()
        createProfileDialogFragment.show(supportFragmentManager, "CreateProfileDialog")
    }

    // Activates the function which shows EditProfileDialogFragment
    private fun showEditProfileDialog() {
        val editProfileDialogFragment = EditProfileDialogFragment()
        editProfileDialogFragment.show(supportFragmentManager, "EditProfileDialog")
    }

    // Activates the function which inflates the menu and adds items to the action bar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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

}
