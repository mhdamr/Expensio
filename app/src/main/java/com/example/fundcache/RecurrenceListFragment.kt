package com.example.fundcache

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction

class RecurrenceListFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var walletId: String
    private lateinit var recurrenceListRecyclerView: RecyclerView
    private lateinit var adapter: RecurrenceListAdapter
    private lateinit var noRecurrenceTransactionsText: TextView
    private lateinit var noRecurrenceTransactionsImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recurrence_list, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        walletId = arguments?.getString("walletId") ?: ""

        recurrenceListRecyclerView = view.findViewById(R.id.recurrence_list_recyclerview)

        // Hide the bottom app bar
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.GONE

        // Initialize the adapter for the RecyclerView
        adapter = RecurrenceListAdapter(mutableListOf()) { transactionId ->
            deleteTransaction(transactionId)
        }
        recurrenceListRecyclerView.adapter = adapter
        recurrenceListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Get a reference to the TextView
        noRecurrenceTransactionsText = view.findViewById(R.id.no_recurrence_transactions_text)
        noRecurrenceTransactionsImage = view.findViewById(R.id.no_recurrence_transactions_image)
        fetchRecurrenceTransactions()

        return view
    }

    private fun fetchRecurrenceTransactions() {
        db.collection("users")
            .document(currentUser.uid)
            .collection("wallets")
            .document(walletId)
            .collection("recurrence")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val recurrenceTransactions = mutableListOf<RecurrenceTransaction>()
                querySnapshot.forEach { documentSnapshot ->
                    val transaction = documentSnapshot.toObject(RecurrenceTransaction::class.java)
                    transaction.id = documentSnapshot.id
                    recurrenceTransactions.add(transaction)
                }
                adapter.setRecurrenceTransactions(recurrenceTransactions)

                // Update the visibility of the TextView based on the number of transactions
                if (recurrenceTransactions.isEmpty()) {
                    noRecurrenceTransactionsText.visibility = View.VISIBLE
                    noRecurrenceTransactionsImage.visibility = View.VISIBLE
                } else {
                    noRecurrenceTransactionsText.visibility = View.GONE
                    noRecurrenceTransactionsImage.visibility = View.GONE
                }
            }
    }

    private fun deleteTransaction(transactionId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Recurring Transaction")
        builder.setMessage("Are you sure you want to delete this recurrence?")
        builder.setPositiveButton("Delete") { _, _ ->
            db.collection("users")
                .document(currentUser.uid)
                .collection("wallets")
                .document(walletId)
                .collection("recurrence")
                .document(transactionId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Transaction deleted.", Toast.LENGTH_SHORT).show()
                    fetchRecurrenceTransactions()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error deleting transaction.", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show the bottom app bar when leaving the fragment
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.VISIBLE
    }
}
