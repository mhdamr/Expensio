package com.example.expensio.Transactions

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensio.R
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchTransactionFragment : Fragment() {
    private lateinit var searchEditText: EditText
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var transactionAdapter: SearchTransactionAdapter
    private val transactions: MutableList<SearchedTransactionItem> = mutableListOf()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_transaction, container, false)

        loadTransactions()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the bottom app bar
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.GONE

        searchEditText = view.findViewById(R.id.searchEditText)
        transactionsRecyclerView = view.findViewById(R.id.transactionsRecyclerView)

        transactionAdapter = SearchTransactionAdapter(transactions)
        transactionsRecyclerView.adapter = transactionAdapter
        transactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                searchTransactions(s.toString())
            }

            override fun afterTextChanged(s: Editable) {}
        })


    }

    private fun loadTransactions() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .collection("wallets")
                .get()
                .addOnSuccessListener { walletSnapshot ->
                    walletSnapshot.documents.forEach { walletDocument ->
                        val walletId = walletDocument.id
                        val walletName = walletDocument.getString("name") ?: ""

                        walletDocument.reference.collection("transactions")
                            .get()
                            .addOnSuccessListener { transactionSnapshot ->
                                transactionSnapshot.documents.forEach { transactionDocument ->
                                    val description = transactionDocument.getString("description") ?: ""
                                    val amount = transactionDocument.getDouble("amount") ?: 0.00
                                    val timestamp = transactionDocument.getDate("timestamp")
                                    val transactionType = transactionDocument.getString("type") ?: ""

                                    if (timestamp != null) {
                                        transactions.add(
                                            SearchedTransactionItem(
                                                walletId,
                                                walletName,
                                                description,
                                                amount,
                                                timestamp,
                                                transactionType
                                            )
                                        )
                                    }
                                }

                                transactionAdapter.notifyDataSetChanged()

                                // Check if transactions list is empty
                                if (transactions.isEmpty()) {
                                    view?.findViewById<LinearLayout>(R.id.noTransactionsLayout)
                                        ?.visibility = View.VISIBLE
                                } else {
                                    view?.findViewById<LinearLayout>(R.id.noTransactionsLayout)
                                        ?.visibility = View.GONE
                                }

                            }
                    }
                }
        }
    }

    private fun searchTransactions(query: String) {
        val filteredTransactions = transactions.filter { transaction ->
            transaction.description.contains(query, ignoreCase = true) ||
                    transaction.amount.toString().contains(query)
        }

        // Set visibility of noTransactionsLayout based on filtered transactions
        val noTransactionsLayout = view?.findViewById<LinearLayout>(R.id.noTransactionsLayout)
        if (filteredTransactions.isEmpty()) {
            noTransactionsLayout?.visibility = View.VISIBLE
        } else {
            noTransactionsLayout?.visibility = View.GONE
        }

        transactionAdapter = SearchTransactionAdapter(filteredTransactions)
        transactionsRecyclerView.adapter = transactionAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show the bottom app bar when leaving the fragment
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.VISIBLE
    }
}