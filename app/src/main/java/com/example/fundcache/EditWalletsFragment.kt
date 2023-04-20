package com.example.fundcache

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fundcache.databinding.FragmentEditWalletsBinding
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.listener.ColorListener
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditWalletsFragment : Fragment() {

    private lateinit var binding: FragmentEditWalletsBinding
    private lateinit var walletId: String
    private lateinit var walletNameEditText: EditText
    private lateinit var currencyEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var colorButton: Button
    private var selectedColor = "#2196f3"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditWalletsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the bottom app bar
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.GONE

        walletId = arguments?.getString("walletId") ?: ""

        // Find the EditText view
        val currencyEditText = view.findViewById<EditText>(R.id.currency_edittext)

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

        walletNameEditText = view.findViewById(R.id.wallet_name_edittext)
        amountEditText = view.findViewById(R.id.amount_edittext)
        colorButton = view.findViewById(R.id.color_button)

        val saveButton = view.findViewById<Button>(R.id.save_button)

        // Set the current values of the wallet in the EditTexts
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).collection("wallets")
                .document(walletId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        walletNameEditText.setText(document.getString("name"))
                        currencyEditText.setText(document.getString("currency"))
                        amountEditText.setText(document.getDouble("amount").toString())
                        colorButton.setBackgroundColor(Color.parseColor(document.getString("color")))
                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }

        // Set up the color picker button
        colorButton.setOnClickListener {
            ColorPickerDialog
                .Builder(requireContext())
                .setTitle("Select a Color")
                .setColorShape(ColorShape.SQAURE)
                .setDefaultColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                .setColorListener(object : ColorListener {
                    override fun onColorSelected(color: Int, colorHex: String) {
                        selectedColor = colorHex
                        colorButton.setBackgroundColor(color)
                    }
                })
                .show()
        }

        // Set up the save button
        saveButton.setOnClickListener {
            val walletName = walletNameEditText.text.toString().trim()
            val currency = currencyEditText.text.toString().trim()
            val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0

            if (walletName.isNotEmpty() && currency.isNotEmpty() && currentUser != null) {
                val wallet = hashMapOf(
                    "name" to walletName,
                    "currency" to currency,
                    "amount" to amount,
                    "color" to selectedColor
                )

                // Update the wallet data in the document with the ID of the current user and the ID of the wallet
                db.collection("users").document(currentUser.uid)
                    .collection("wallets")
                    .document(walletId)
                    .update(wallet as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Wallet updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(R.id.walletsFragment)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error updating wallet", e)
                        Toast.makeText(
                            requireContext(),
                            "Failed to update wallet. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }


    }


    private fun showDeleteWalletDialog() {
        if (currentUser != null) {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Wallet")
                .setMessage("Are you sure you want to delete this wallet?")
                .setPositiveButton("Delete") { dialog, _ ->
                    db.collection("users").document(currentUser.uid)
                        .collection("wallets")
                        .document(walletId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Wallet deleted successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigate(R.id.walletsFragment)
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error deleting wallet", e)
                            Toast.makeText(
                                requireContext(),
                                "Failed to delete wallet. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_wallet -> {
                showDeleteWalletDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show the bottom app bar when leaving the fragment
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.VISIBLE
    }

}