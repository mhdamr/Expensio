package com.example.expensio

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensio.databinding.FragmentTransactionListBinding
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class TransactionListFragment : Fragment(R.layout.fragment_transaction_list), TransactionsAdapter.OnTransactionUpdatedListener, TransactionsAdapter.OnWalletBalanceUpdatedListener {
    private var _binding: FragmentTransactionListBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    private lateinit var walletId: String
    private var currentUser: FirebaseUser? = null
    private var year: Int = 0
    private var month: Int = 0
    private var onTransactionListWalletBalanceUpdatedListener: OnTransactionListWalletBalanceUpdatedListener? = null

    private lateinit var transactionsAdapter: TransactionsAdapter

    interface OnTransactionListWalletBalanceUpdatedListener {
        fun onTransactionListWalletBalanceUpdated()
    }

    override fun onWalletBalanceUpdated() {
        onTransactionListWalletBalanceUpdatedListener?.onTransactionListWalletBalanceUpdated()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onTransactionListWalletBalanceUpdatedListener = parentFragment as OnTransactionListWalletBalanceUpdatedListener
        } catch (e: ClassCastException) {
            throw ClassCastException(parentFragment.toString() + " must implement OnTransactionListWalletBalanceUpdatedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        onTransactionListWalletBalanceUpdatedListener = null
    }

    companion object {
        private const val ARG_WALLET_ID = "wallet_id"
        private const val ARG_CURRENT_USER = "current_user"
        private const val ARG_YEAR = "year"
        private const val ARG_MONTH = "month"

        fun newInstance(walletId: String, currentUser: FirebaseUser?, year: Int, month: Int): TransactionListFragment {
            val args = Bundle()
            args.putString(ARG_WALLET_ID, walletId)
            args.putParcelable(ARG_CURRENT_USER, currentUser)
            args.putInt(ARG_YEAR, year)
            args.putInt(ARG_MONTH, month)

            val fragment = TransactionListFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            walletId = it.getString(ARG_WALLET_ID) ?: ""
            currentUser = it.getParcelable(ARG_CURRENT_USER)
            year = it.getInt(ARG_YEAR)
            month = it.getInt(ARG_MONTH)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTransactionListBinding.bind(view)

        // Set up the transactions RecyclerView and adapter
        transactionsAdapter = TransactionsAdapter(requireContext(), walletId, this, this)
        binding.transactionsRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionsRecyclerview.adapter = transactionsAdapter

        // Load the transactions for the specified month
        loadTransactionsForMonth(year, month)
    }

    override fun onTransactionUpdated() {
        loadTransactionsForMonth(year, month)
    }

    private fun loadTransactionsForMonth(year: Int, month: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        val monthStartDate = calendar.time
        calendar.set(year, month + 1, 1, 0, 0, 0)
        val monthEndDate = calendar.time

        val transactionsRef = db.collection("users").document(currentUser?.uid ?: "")
            .collection("wallets").document(walletId)
            .collection("transactions")

        transactionsRef.whereGreaterThanOrEqualTo("timestamp", monthStartDate)
            .whereLessThan("timestamp", monthEndDate)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val transactions = mutableListOf<TransactionListItem>()
                var lastDate: Date? = null
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                for (document in querySnapshot.documents) {
                    val transaction = document.toObject(TransactionItem::class.java)
                    if (transaction != null) {
                        transaction.id = document.id

                        // Format the transaction timestamp to remove the time portion
                        val transactionDate = dateFormat.parse(dateFormat.format(transaction.timestamp))

                        // Check if the transaction date is different from the last transaction date
                        if (transactionDate != lastDate) {
                            // Add a DateHeader with the new date
                            transactions.add(TransactionListItem.DateHeader(dateFormat.format(transactionDate)))
                            lastDate = transactionDate
                        }

                        transactions.add(TransactionListItem.Transaction(transaction))
                    }
                }

                if (transactions.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    binding.emptyView.visibility = View.GONE
                }

                transactionsAdapter.submitList(transactions)

            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}