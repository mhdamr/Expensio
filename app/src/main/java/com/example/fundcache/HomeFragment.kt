package com.example.fundcache

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fundcache.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController
import com.example.fundcache.R.id.action_homeFragment_to_walletsFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val binding get() = _binding!!
    private lateinit var dialog: Dialog
    private var dialogIsDisplayed: Boolean = false
    private var selectedProfilePictureUri: Uri? = null
    private val REQUEST_IMAGE_PICK = 1
    private val PERMISSION_REQUEST_CODE = 1001
    private lateinit var imgProfilePicture: ImageView



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Get a reference to the FirebaseAuth and Firebase Firestore instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if the user has a name in Firestore
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = db.collection("users").document(currentUser.uid)
            userRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    val name = documentSnapshot.getString("name")
                    if (name.isNullOrEmpty() || name.length < 1 || !name.matches(Regex("^(?=.*[a-zA-Z0-9]).+\$"))) {
                        // Show a dialog to prompt the user to enter a name
                        if (!dialogIsDisplayed) {
                            showDialog()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Show an error message
                    Log.e("HomeFragment", "Error retrieving user name: ${e.message}", e)
                }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up any UI or functionality for the fragment here
        binding.createWalletButton.setOnClickListener {
            findNavController().navigate(action_homeFragment_to_walletsFragment)
        }
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted, open the image picker
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        } else {
            // Permission is not granted, show a message to the user
            Toast.makeText(requireContext(), "Permission to access storage is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedProfilePictureUri = data?.data
            dialog.findViewById<ImageView>(R.id.selectProfilePictureButton)?.setImageURI(selectedProfilePictureUri)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, open the image picker
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                pickImageLauncher.launch(intent)
            } else {
                // Permission is not granted, show a message to the user
                Toast.makeText(requireContext(), "Permission to access storage is required", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showDialog() {
        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_choose_name)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnSave = dialog.findViewById<Button>(R.id.saveName)
        val txtName = dialog.findViewById<EditText>(R.id.nameEditText)
        val imgProfilePicture = dialog.findViewById<ImageView>(R.id.selectProfilePictureButton)

        dialogIsDisplayed = true

        imgProfilePicture.setOnClickListener {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                requestPermissionLauncher.launch(permission)
            } else {
                // Permission is granted, open the image picker
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                pickImageLauncher.launch(intent)
            }
        }

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
                if (selectedProfilePictureUri != null) {
                    userMap["profilePicture"] = selectedProfilePictureUri.toString()
                }
                db.collection("users").document(currentUser.uid)
                    .set(userMap, options)
                    .addOnSuccessListener {
                        dialogIsDisplayed = false
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        dialogIsDisplayed = false
                        Log.e("HomeFragment", "Error saving name: ${e.message}", e)
                    }
            }
        }

        dialog.show()
    }

}