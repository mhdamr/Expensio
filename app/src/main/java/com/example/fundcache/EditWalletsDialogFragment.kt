package com.example.fundcache

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.fundcache.databinding.FragmentEditWalletsDialogBinding
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.listener.ColorListener
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditWalletsDialogFragment : DialogFragment() {

    private lateinit var walletId: String
    private lateinit var walletNameEditText: EditText
    private lateinit var currencyEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var colorButton: Button
    private var selectedColor = "#2196f3"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    @SuppressLint("ResourceType")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.fragment_edit_wallets_dialog)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        walletId = arguments?.getString("walletId") ?: ""

        walletNameEditText = dialog.findViewById(R.id.wallet_name_edittext)
        currencyEditText = dialog.findViewById(R.id.currency_edittext)
        amountEditText = dialog.findViewById(R.id.amount_edittext)
        colorButton = dialog.findViewById(R.id.color_button)

        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
        val saveButton = dialog.findViewById<Button>(R.id.save_button)

        // Set the dialog title
        dialog?.setTitle("Edit Wallet")

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

        // Set up the cancel button
        cancelButton.setOnClickListener {
            dismiss()
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
                        dismiss()
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error updating wallet", e)
                        Toast.makeText(
                            requireContext(),
                            "Failed to update wallet.Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        parentFragmentManager.setFragmentResult(
            "refreshWallets",
            bundleOf("refresh" to true)
        )
    }
}