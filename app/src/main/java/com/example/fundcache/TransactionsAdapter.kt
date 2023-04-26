package com.example.fundcache

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fundcache.databinding.DateHeaderBinding
import com.example.fundcache.databinding.TransactionItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

sealed class TransactionListItem {
    data class DateHeader(val date: String) : TransactionListItem()
    data class Transaction(val transaction: TransactionItem) : TransactionListItem()
}

class TransactionsAdapter(
    private val context: Context,
    private val walletId: String,
    private val onTransactionUpdatedListener: OnTransactionUpdatedListener
    ) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(TransactionDiffCallback()) {

    interface OnTransactionUpdatedListener {
        fun onTransactionUpdated()
    }

    private val HEADER_VIEW_TYPE = 1
    private val TRANSACTION_VIEW_TYPE = 2

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionListItem.DateHeader -> HEADER_VIEW_TYPE
            is TransactionListItem.Transaction -> TRANSACTION_VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER_VIEW_TYPE -> {
                val binding = DateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateHeaderViewHolder(binding)
            }
            else -> {
                val binding = TransactionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TransactionViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TransactionListItem.DateHeader -> {
                val viewHolder = holder as DateHeaderViewHolder
                viewHolder.binding.dateHeaderText.text = SimpleDateFormat("MMMM dd", Locale.getDefault()).format(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(item.date))
            }
            is TransactionListItem.Transaction -> {
                val viewHolder = holder as TransactionViewHolder
                val transaction = item.transaction
                with(viewHolder.binding) {
                    transactionType.text = transaction.type.capitalize(Locale.getDefault())
                    transactionDescription.text = transaction.description
                    transactionAmount.text = String.format("%.2f", transaction.amount)

                    if (transaction.type == "expense") {
                        transactionAmount.setTextColor(ContextCompat.getColor(context, R.color.Expense))
                        transactionType.text = "-"
                    } else {
                        transactionAmount.setTextColor(ContextCompat.getColor(context, R.color.Income))
                        transactionType.text = "+"
                    }
                }
            }
        }
    }

    inner class DateHeaderViewHolder(val binding: DateHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    inner class TransactionViewHolder(val binding: TransactionItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val transaction = (getItem(adapterPosition) as? TransactionListItem.Transaction)?.transaction
                transaction?.let {
                    showTransactionDetailsDialog(it)
                }
            }
        }
    }
    private fun showTransactionDetailsDialog(transaction: TransactionItem) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_edit_transaction, null)
        builder.setView(dialogView)

        val amountEditText = dialogView.findViewById<EditText>(R.id.edit_amount)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.edit_description)
        val typeTextView = dialogView.findViewById<TextView>(R.id.edit_type)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)
        val saveChangesButton = dialogView.findViewById<Button>(R.id.save_changes_btn)

        // Set the transaction data in the edit fields
        amountEditText.setText(transaction.amount.toString())
        descriptionEditText.setText(transaction.description)

        if (transaction.type == "expense"){
            typeTextView.text = "Expense"
            typeTextView.setTextColor(Color.parseColor("#EE3434"))
        }else{
            typeTextView.text = "Income"
            typeTextView.setTextColor(Color.parseColor("#39FA41"))
        }



        val alertDialog = builder.create()

        // Handle cancel button click
        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        // Handle save changes button click
        saveChangesButton.setOnClickListener {
            // Save changes to the transaction
            updateTransaction(transaction.id,transaction.amount, amountEditText.text.toString(), descriptionEditText.text.toString(),typeTextView.text.toString())
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun updateTransaction(transactionId: String, oldAmount: Double,newAmount: String, newDescription: String, oldTransactionType: String) {
        val updatedTransaction = mapOf(
            "amount" to newAmount.toDouble(),
            "description" to newDescription,
        )

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val walletRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("wallets")
                .document(walletId)

            FirebaseFirestore.getInstance().runTransaction { transaction ->
                // Get the current wallet data
                val walletDoc = transaction.get(walletRef)
                val currentAmount = walletDoc.getDouble("amount") ?: 0.0

                // Update the transaction document
                transaction.update(
                    walletRef.collection("transactions").document(transactionId),
                    updatedTransaction
                )

                // Update the wallet amount
                val newWalletAmount = when (oldTransactionType) {
                    "Income" -> currentAmount - oldAmount + newAmount.toDouble()
                    "Expense" -> currentAmount + oldAmount - newAmount.toDouble()
                    else -> currentAmount
                }
                transaction.update(walletRef, "amount", newWalletAmount)
            }
                .addOnSuccessListener {
                    Toast.makeText(context, "Transaction updated successfully.", Toast.LENGTH_SHORT).show()
                    onTransactionUpdatedListener.onTransactionUpdated()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error updating transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {
        override fun areItemsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
            if (oldItem is TransactionListItem.DateHeader && newItem is TransactionListItem.DateHeader) {
                return oldItem.date == newItem.date
            }
            if (oldItem is TransactionListItem.Transaction && newItem is TransactionListItem.Transaction) {
                return oldItem.transaction.id == newItem.transaction.id
            }
            return false
        }

        override fun areContentsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
            return oldItem == newItem
        }
    }
}