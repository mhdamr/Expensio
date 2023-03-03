package com.example.fundcache

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fundcache.databinding.FragmentAddWalletsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddWalletsFragment : Fragment() {
    private lateinit var binding: FragmentAddWalletsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddWalletsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up currency spinner
        val currencies = arrayOf("USD", "EUR", "GBP", "JPY")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.currencySpinner.adapter = adapter

        binding.createWalletButton.setOnClickListener {
            val walletName = binding.walletNameEditText.text.toString().trim()
            val currency = binding.currencySpinner.selectedItem.toString().trim()
            val amount = binding.amountEditText.text.toString().toDoubleOrNull() ?: 0.0

            if (walletName.isNotEmpty() && currency.isNotEmpty() && currentUser != null) {
                val wallet = hashMapOf(
                    "name" to walletName,
                    "currency" to currency,
                    "amount" to amount
                )

                // Add the wallet data to the document with the ID of the current user
                db.collection("users").document(currentUser.uid)
                    .collection("wallets")
                    .add(wallet)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Wallet created successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Navigate back to the wallet list fragment
                        findNavController().navigate(R.id.action_addWalletsFragment_to_walletsFragment)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding wallet", e)
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