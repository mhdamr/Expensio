package com.example.fundcache

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fundcache.databinding.FragmentChooseNameBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ChooseNameFragment : Fragment() {

    private lateinit var binding: FragmentChooseNameBinding
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChooseNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (currentUser != null) {
            val userRef = db.collection("users").document(currentUser.uid)
            userRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    val name = documentSnapshot.getString("name")
                    if (name == null) {
                        binding.submitButton.setOnClickListener {
                            val name = binding.nameEditText.text.toString()
                            if (name.isBlank()) {
                                // Show an error message if name is blank
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Error")
                                    .setMessage("Please enter a name.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            } else {
                                val data = hashMapOf("name" to name)
                                userRef.set(data, SetOptions.merge())
                                    .addOnSuccessListener {
                                        // Navigate back to the ProfileFragment
                                        findNavController().navigateUp()
                                    }
                                    .addOnFailureListener { e ->
                                        // Show an error message
                                        MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("Error")
                                            .setMessage("Failed to save your name. Please try again later.")
                                            .setPositiveButton("OK", null)
                                            .show()
                                    }
                            }
                        }
                    } else {
                        // User already has a name set, navigate back to the ProfileFragment
                        findNavController().navigateUp()
                    }
                }
                .addOnFailureListener { e ->
                    // Show an error message
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Error")
                        .setMessage("Failed to retrieve your name. Please try again later.")
                        .setPositiveButton("OK", null)
                        .show()
                }
        }
    }
}