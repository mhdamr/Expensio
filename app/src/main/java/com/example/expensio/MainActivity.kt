package com.example.expensio

import LockActivity
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LightingColorFilter
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.expensio.databinding.ActivityMainBinding
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.droidsonroids.gif.GifImageView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    lateinit var toggle : ActionBarDrawerToggle
    private lateinit var fab: FloatingActionButton
    private lateinit var fab1: FloatingActionButton
    private lateinit var fab3: FloatingActionButton
    private lateinit var fab1_label: TextView
    private lateinit var fab3_label: TextView
    private var menuMain: Menu? = null
    private val alphaHide = 0f
    private val scaleHide = 0f
    private var isAppLocked = false
    private lateinit var appLockManager: LockActivity
    private lateinit var lockSwitch: Switch

    private lateinit var networkChangeReceiver: BroadcastReceiver

    // Show a pop-up when internet connection is lost
    private fun showNoInternetDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_no_internet, null)
        val retryButton = dialogView.findViewById<Button>(R.id.retry_button)

        val gifImageView = dialogView.findViewById<GifImageView>(R.id.gif_image)
        val color = ContextCompat.getColor(this, R.color.colorTextPrimary)
        val colorFilter: ColorFilter = LightingColorFilter(Color.RED, color)
        gifImageView.colorFilter = colorFilter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        retryButton.setOnClickListener {
            if (!isOnline()) {
                // Show pop-up again
                dialog.dismiss()
                showNoInternetDialog()
            } else {
                // Close the pop-up and allow the user to use the app normally
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // Check if the device is connected to the internet
    private fun isOnline(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isAvailable && activeNetworkInfo.isConnected
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Register the network change receiver
        networkChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Check if the network connection is lost
                if (!isOnline()) {
                    // Show pop-up
                    showNoInternetDialog()
                }
            }
        }

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)

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
                navController.navigate(R.id.action_homeFragment)
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

        // Set the function of the hamburger menu to open/close the Navigation Drawer
        toggle = ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.open,R.string.close)
        drawerLayout.addDrawerListener(toggle)


        // Set the items in the Navigation Drawer
        navView.setNavigationItemSelectedListener {

            when(it.itemId) {

                /*R.id.drawer_bank_sync ->
                    Toast.makeText(applicationContext,"Clicked Home", Toast.LENGTH_SHORT).show()*/
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
            R.id.homeFragment, R.id.walletsFragment)),drawerLayout)
        toolbar.setupWithNavController(navController, appBarConfiguration)

        // Find the Bottom Navigation View
        val bottomNav : BottomNavigationView = findViewById(R.id.bottomNav)

        // Set the items in the Bottom Navigation View
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    navController.navigate(R.id.action_homeFragment)
                    true
                }
                R.id.wallets -> {
                    navController.navigate(R.id.action_walletsFragment)
                    true
                }
                else -> {
                    true
                }
            }
        }
        bottomNav.background = null
        bottomNav.menu.getItem(1).isEnabled = false

        val bottomAppBar : BottomAppBar = findViewById(R.id.bottomAppBar)

        fab = findViewById(R.id.floatingActionButton)
        fab1 = findViewById(R.id.fab1)
        fab3 = findViewById(R.id.fab3)
        fab1_label = findViewById(R.id.fab1_label)
        fab3_label = findViewById(R.id.fab3_label)

        // Add the destination change listener
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.walletsFragment -> {
                    toggle.setDrawerIndicatorEnabled(true)
                    toolbar.setNavigationIcon(R.drawable.icon_ham_menu)
                }
                else -> {
                    toggle.setDrawerIndicatorEnabled(false)
                    toolbar.setNavigationIcon(R.drawable.icon_back)
                }
            }

            when (destination.id) {
                R.id.walletsFragment -> {
                    fab.show()
                    fab1.hide()
                    fab3.hide()

                    fab1_label.visibility = View.GONE
                    fab3_label.visibility = View.GONE

                    fab.setImageResource(R.drawable.icon_add)
                    fab1.setImageResource(R.drawable.icon_income)
                    fab3.setImageResource(R.drawable.icon_expense)

                    fab.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#bc00ff"))
                    fab.animate().rotation(0f).setDuration(200).start()

                    fab1.animate().translationX(0f).translationY(0f).alpha(0f).scaleX(0f).scaleY(0f)
                    fab3.animate().translationX(0f).translationY(0f).alpha(0f).scaleX(0f).scaleY(0f)

                    fab1_label.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)
                    fab3_label.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)
                    bottomAppBar.fabCradleMargin = 30f
                    bottomAppBar.fabCradleRoundedCornerRadius = 30f
                    bottomAppBar.cradleVerticalOffset = 30f

                    bottomNav.menu.findItem(R.id.placeholder).isVisible = true
                }
                R.id.homeFragment -> {
                    fab.show()
                    fab1.hide()
                    fab3.hide()

                    fab1_label.visibility = View.GONE
                    fab3_label.visibility = View.GONE

                    fab.setImageResource(R.drawable.icon_search)
                    fab1.setImageResource(R.drawable.icon_income)
                    fab3.setImageResource(R.drawable.icon_expense)

                    fab.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#bc00ff"))
                    fab.animate().rotation(0f).setDuration(200).start()

                    fab1.animate().translationX(0f).translationY(0f).alpha(0f).scaleX(0f).scaleY(0f)
                    fab3.animate().translationX(0f).translationY(0f).alpha(0f).scaleX(0f).scaleY(0f)

                    fab1_label.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)
                    fab3_label.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)
                    bottomAppBar.fabCradleMargin = 30f
                    bottomAppBar.fabCradleRoundedCornerRadius = 30f
                    bottomAppBar.cradleVerticalOffset = 30f

                    bottomNav.menu.findItem(R.id.placeholder).isVisible = true
                }
                R.id.walletDetailFragment -> {
                    fab.show()

                    bottomAppBar.fabCradleMargin = 30f
                    bottomAppBar.fabCradleRoundedCornerRadius = 30f
                    bottomAppBar.cradleVerticalOffset = 30f

                    fab1.setImageResource(R.drawable.icon_income)
                    fab3.setImageResource(R.drawable.icon_expense)

                    bottomNav.menu.findItem(R.id.placeholder).isVisible = true
                }
                else -> {
                    fab.hide()
                    fab1.hide()
                    fab3.hide()

                    fab1_label.visibility = View.GONE
                    fab3_label.visibility = View.GONE

                    fab1_label.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)
                    fab3_label.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)

                    fab.setImageResource(R.drawable.icon_add)
                    fab1.setImageResource(R.drawable.icon_income)
                    fab3.setImageResource(R.drawable.icon_expense)

                    fab.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#bc00ff"))
                    fab.animate().rotation(0f).setDuration(200).start()

                    fab1.animate().translationX(0f).translationY(0f).alpha(0f).scaleX(0f).scaleY(0f)
                    fab3.animate().translationX(0f).translationY(0f).alpha(0f).scaleX(0f).scaleY(0f)
                    bottomNav.menu.findItem(R.id.placeholder).isVisible = false
                    bottomAppBar.fabCradleMargin = 0f
                    bottomAppBar.fabCradleRoundedCornerRadius = 0f
                    bottomAppBar.cradleVerticalOffset = 0f
                }
            }
        }


        // Show the Create Profile Dialog if the user does not have a display name
        if (currentUser != null && currentUser.isEmailVerified) {

            navController.navigate(R.id.action_homeFragment)

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

        appLockManager = LockActivity(this)

        lockSwitch = navView.menu.findItem(R.id.lock_app_switch).actionView as Switch
        lockSwitch.isChecked = appLockManager.isAppLocked()

        lockSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                appLockManager.showFingerprintPrompt(
                    onSuccess = { appLockManager.setAppLocked(true) },
                    onError = { lockSwitch.isChecked = false }
                )
            } else {
                appLockManager.setAppLocked(false)
            }
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

    override fun onBackPressed() {
        // Get the current fragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)

        // Check if the current fragment is a child of the HomeFragment
        if (currentFragment is HomeFragment) {
            // Exit the app if the user is on the HomeFragment
            super.onBackPressed()
        } else {
            // Navigate back if the user is not on the HomeFragment
            navController.navigateUp()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menuMain = menu

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val currentDestination = navController.currentDestination?.id

        when (currentDestination) {
            R.id.walletDetailFragment -> {
                menu.findItem(R.id.edit_wallet).isVisible = true
                menu.findItem(R.id.action_delete_wallet).isVisible = false
            }

            R.id.editWalletsFragment -> {
                menu.findItem(R.id.edit_wallet).isVisible = false
                menu.findItem(R.id.action_delete_wallet).isVisible = true
            }

            else -> {
                menu.findItem(R.id.edit_wallet).isVisible = false
                menu.findItem(R.id.action_delete_wallet).isVisible = false
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.lock_app_switch -> {
                toggleAppLock()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun toggleAppLock() {
        if (appLockManager.isAppLocked()) {
            showFingerprintPrompt()
        } else {
            appLockManager.setAppLocked(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun showFingerprintPrompt() {
        appLockManager.showFingerprintPrompt(
            onSuccess = { unlockApp() },
            onError = { /* Handle error */ }
        )
    }

    private fun unlockApp() {
        appLockManager.setAppLocked(false)
    }


}
