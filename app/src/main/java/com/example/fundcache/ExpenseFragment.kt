package com.example.fundcache

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ExpenseFragment : Fragment() {
    private lateinit var amountEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var walletId: String
    private var walletBalance: Double = 0.0
    private lateinit var recurrenceSpinner: Spinner
    private lateinit var recurrenceOption: String


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expense, container, false)

        // Get a reference to the Firebase Firestore and FirebaseAuth objects
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        // Get the walletId argument passed from WalletDetailFragment
        walletId = arguments?.getString("walletId") ?: ""
        walletBalance = arguments?.getDouble("walletBalance") ?: 0.0

        // Get references to the UI components
        amountEditText = view.findViewById(R.id.amount_edittext)
        descriptionEditText = view.findViewById(R.id.description_edittext)
        saveButton = view.findViewById(R.id.save_button)

        // Set a click listener for the save button
        saveButton.setOnClickListener {
            saveExpense()
        }

        recurrenceSpinner = view.findViewById(R.id.recurrence_spinner)
        recurrenceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                recurrenceOption = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                recurrenceOption = "Never"
            }
        }


        Log.d("MyApp", "Wallet ID: $walletId")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the bottom app bar
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.GONE
    }

    private fun saveExpense() {
        val amount = amountEditText.text.toString().toDoubleOrNull()
        val description = descriptionEditText.text.toString()

        // Validate the input
        if (amount == null || description.isEmpty()) {
            Toast.makeText(context, "Please enter valid expense data.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new expense object
        val expense = hashMapOf(
            "amount" to amount,
            "description" to description,
            "type" to "expense",
            "timestamp" to FieldValue.serverTimestamp(),
        )


        // Add the expense to the selected wallet's transactions collection
        db.collection("users").document(currentUser.uid)
            .collection("wallets")
            .document(walletId)
            .collection("transactions")
            .add(expense)
            .addOnSuccessListener {
                Toast.makeText(context, "Expense added successfully.", Toast.LENGTH_SHORT).show()
                if (recurrenceOption != "Never") {
                    saveRecurrence()
                }

                // Get the wallet balance from the document snapshot and update the UI
                db.collection("users").document(currentUser.uid)
                    .collection("wallets")
                    .document(walletId)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val walletAmount = documentSnapshot.getDouble("amount")
                        walletBalance = walletAmount ?: 0.0 // Update the wallet balance property with the retrieved value

                        db.collection("users").document(currentUser.uid)
                            .collection("wallets")
                            .document(walletId)
                            .update("amount", (walletBalance - amount))
                    }.addOnSuccessListener {
                        activity?.onBackPressed()
                    }


            }
            .addOnFailureListener {
                Toast.makeText(context, "Error adding expense.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveRecurrence() {
        val amount = amountEditText.text.toString().toDoubleOrNull()
        val description = descriptionEditText.text.toString()

        // Validate the input
        if (amount == null || description.isEmpty()) {
            Toast.makeText(context, "Please enter valid expense data.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new expense object
        val expense = hashMapOf(
            "amount" to amount,
            "description" to description,
            "type" to "expense",
            "timestamp" to FieldValue.serverTimestamp(),
            "recurrence" to recurrenceOption
        )


        // Add the expense to the selected wallet's transactions collection
        db.collection("users").document(currentUser.uid)
            .collection("wallets")
            .document(walletId)
            .collection("recurrence")
            .add(expense)
/*            .addOnSuccessListener {
                Toast.makeText(context, "expense added successfully.", Toast.LENGTH_SHORT).show()
                activity?.onBackPressed()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error adding expense.", Toast.LENGTH_SHORT).show()
            }*/
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show the bottom app bar when leaving the fragment
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.VISIBLE
    }

}