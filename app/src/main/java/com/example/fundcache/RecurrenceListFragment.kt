package com.example.fundcache

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

// Initialize the adapter for the RecyclerView
        adapter = RecurrenceListAdapter(mutableListOf()) { transactionId ->
            deleteTransaction(transactionId)
        }
        recurrenceListRecyclerView.adapter = adapter
        recurrenceListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

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
            }
    }

    private fun deleteTransaction(transactionId: String) {
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
}