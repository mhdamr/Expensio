package com.example.expensio.Wallets

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.expensio.databinding.FragmentAddWalletsBinding
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.ContextCompat
import com.example.expensio.R
import com.github.dhaval2404.colorpicker.listener.ColorListener
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.bottomappbar.BottomAppBar

class AddWalletsFragment : Fragment() {
    private lateinit var binding: FragmentAddWalletsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private var selectedColor = "#2196f3"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddWalletsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the bottom app bar
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.GONE

        // Find the EditText view
        val currencyEditText = view.findViewById<EditText>(R.id.currency_spinner)


        // Set up currency spinner
        val currencies = arrayOf("USD", "EUR", "GBP", "JPY")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set the adapter for the spinner
        currencyEditText.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Select a currency")
            builder.setAdapter(adapter) { _, position ->
                // Set the selected currency in the EditText
                currencyEditText.setText(currencies[position])
            }
            builder.create().show()
        }

        binding.btnSelectColor.setOnClickListener {
            ColorPickerDialog
                .Builder(requireContext())
                .setTitle("Select a Color")
                .setColorShape(ColorShape.SQAURE)
                .setDefaultColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                .setColorListener(object : ColorListener {
                    override fun onColorSelected(color: Int, colorHex: String) {
                        selectedColor = colorHex
                        binding.btnSelectColor.setBackgroundColor(color)
                    }
                })
                .show()
        }

        binding.createWalletButton.setOnClickListener {
            val walletName = binding.walletNameEditText.text.toString().trim()
            val currency = binding.currencySpinner.text.toString().trim()
            val amount = binding.amountEditText.text.toString().toDoubleOrNull() ?: 0.0

            if (walletName.isNotEmpty() && currency.isNotEmpty() && currentUser != null) {
                val wallet = hashMapOf(
                    "name" to walletName,
                    "currency" to currency,
                    "amount" to amount,
                    "color" to selectedColor // <-- Save the selected color
                )

                // Add the wallet data to the document with the ID of the current user
                db.collection("users").document(currentUser.uid)
                    .collection("wallets")
                    .add(wallet)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                requireContext(),
                                "Wallet created successfully!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Navigate back to the wallet list fragment
                            findNavController().navigate(R.id.walletsFragment)
                        } else {
                            Log.w(TAG, "Error adding wallet", task.exception)
                            Toast.makeText(
                                requireContext(),
                                "Failed to create wallet. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show the bottom app bar when leaving the fragment
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.VISIBLE
    }
}