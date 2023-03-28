package com.example.fundcache

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class IncomeFragment : Fragment() {
    private lateinit var amountEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var walletId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_income, container, false)

        // Get a reference to the Firebase Firestore and FirebaseAuth objects
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        // Get the walletId argument passed from WalletDetailFragment
        walletId = arguments?.getString("walletId") ?: ""

        // Get references to the UI components
        amountEditText = view.findViewById(R.id.amount_edittext)
        descriptionEditText = view.findViewById(R.id.description_edittext)
        saveButton = view.findViewById(R.id.save_button)

        // Set a click listener for the save button
        saveButton.setOnClickListener {
            saveIncome()
        }

        Log.d("MyApp", "Wallet ID: $walletId")
        return view
    }

    private fun saveIncome() {
        val amount = amountEditText.text.toString().toDoubleOrNull()
        val description = descriptionEditText.text.toString()

        // Validate the input
        if (amount == null || description.isEmpty()) {
            Toast.makeText(context, "Please enter valid income data.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new income object
        val income = hashMapOf(
            "amount" to amount,
            "description" to description,
            "type" to "income",
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Add the income to the selected wallet's transactions collection
        db.collection("users").document(currentUser.uid)
            .collection("wallets")
            .document(walletId)
            .collection("transactions")
            .add(income)
            .addOnSuccessListener {
                Toast.makeText(context, "Income added successfully.", Toast.LENGTH_SHORT).show()
                activity?.onBackPressed()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error adding income.", Toast.LENGTH_SHORT).show()
            }
    }

}