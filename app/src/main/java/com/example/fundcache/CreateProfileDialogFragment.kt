package com.example.fundcache

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CreateProfileDialogFragment : DialogFragment() {
    private var selectedProfilePictureUri: Uri? =   null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val REQUEST_IMAGE_PICK = 1
    private val PERMISSION_REQUEST_CODE = 1001
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
        val imgProfilePicture = dialog.findViewById<ImageView>(R.id.selectProfilePictureButton)

        val headerView = (requireActivity() as MainActivity).findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)

        userNameTextView = headerView.findViewById(R.id.user_name)

        imgProfilePicture.setOnClickListener {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                requestPermissionLauncher.launch(permission)
            } else {
                // Permission is granted, open the image picker
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted, open the image picker
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
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
            Log.d(TAG, "Received intent: $data")
            selectedProfilePictureUri = data?.data
            Log.d("CreateProfileDialog", "Selected image URI: $selectedProfilePictureUri")
            dialog?.findViewById<ImageView>(R.id.selectProfilePictureButton)?.setImageURI(selectedProfilePictureUri)
        }else {
            Log.w(TAG, "Image picking cancelled or failed with result code ${result.resultCode}")
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

}