package com.example.fundcache

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues.TAG
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
import java.text.SimpleDateFormat
import java.util.*

class IncomeFragment : Fragment() {
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

    private lateinit var dateTimeText: TextView
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private var selectedDateTime: Calendar = Calendar.getInstance()


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
        walletBalance = arguments?.getDouble("walletBalance") ?: 0.0

        // Get references to the UI components
        amountEditText = view.findViewById(R.id.amount_edittext)
        descriptionEditText = view.findViewById(R.id.description_edittext)
        saveButton = view.findViewById(R.id.save_button)
        dateTimeText = view.findViewById(R.id.date_time_text)

        // Set a click listener for the save button
        saveButton.setOnClickListener {
            saveIncome()
        }

        recurrenceSpinner = view.findViewById(R.id.recurrence_spinner)
        recurrenceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                recurrenceOption = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                recurrenceOption = "No Recurrence"
            }
        }


        Log.d("MyApp", "Wallet ID: $walletId")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the default date and time to the current date and time
        updateDateTimeText()

        // Show the date time picker dialog when the date time text is clicked
        dateTimeText.setOnClickListener {
            showDateTimePickerDialog()
        }

        // Hide the bottom app bar
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.GONE

    }

    private fun updateDateTimeText() {
        dateTimeText.text = "${dateFormat.format(selectedDateTime.time)} ${timeFormat.format(selectedDateTime.time)}"
    }

    private fun showDateTimePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDateTime.set(Calendar.YEAR, year)
                selectedDateTime.set(Calendar.MONTH, month)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showTimePickerDialog()
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        )

        // Set the minimum date to today's date
        /*datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000*/

        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDateTime.set(Calendar.MINUTE, minute)
                updateDateTimeText()
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false
        )

        timePickerDialog.show()
    }

    private fun saveIncome() {
        val amount = amountEditText.text.toString().toDoubleOrNull()
        val description = descriptionEditText.text.toString()
        val timestamp = dateTimeText.text.toString()

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
            "timestamp" to Date(timestamp),
        )


        // Add the income to the selected wallet's transactions collection
        db.collection("users").document(currentUser.uid)
            .collection("wallets")
            .document(walletId)
            .collection("transactions")
            .add(income)
            .addOnSuccessListener {
                Toast.makeText(context, "Income added successfully.", Toast.LENGTH_SHORT).show()
                if (recurrenceOption != "No Recurrence") {
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
                            .update("amount", (walletBalance + amount))
                    }.addOnSuccessListener {
                        activity?.onBackPressed()
                    }



            }
            .addOnFailureListener {
                Toast.makeText(context, "Error adding income.", Toast.LENGTH_SHORT).show()
            }

    }

    private fun saveRecurrence() {
        val amount = amountEditText.text.toString().toDoubleOrNull()
        val description = descriptionEditText.text.toString()
        val timestamp = dateTimeText.text.toString()

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
            "timestamp" to Date(timestamp),
            "recurrence" to recurrenceOption
        )


        // Add the income to the selected wallet's transactions collection
        db.collection("users").document(currentUser.uid)
            .collection("wallets")
            .document(walletId)
            .collection("recurrence")
            .add(income)
/*            .addOnSuccessListener {
                Toast.makeText(context, "Income added successfully.", Toast.LENGTH_SHORT).show()
                activity?.onBackPressed()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error adding income.", Toast.LENGTH_SHORT).show()
            }*/
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show the bottom app bar when leaving the fragment
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.VISIBLE
    }

}