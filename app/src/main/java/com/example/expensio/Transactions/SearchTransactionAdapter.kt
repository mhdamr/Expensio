package com.example.expensio.Transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.expensio.R
import java.text.SimpleDateFormat
import java.util.*

class SearchTransactionAdapter(private val transactions: List<SearchedTransactionItem>) :
    RecyclerView.Adapter<SearchTransactionAdapter.SearchTransactionViewHolder>() {

    inner class SearchTransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val walletNameTextView: TextView = itemView.findViewById(R.id.walletNameTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        val transactionTypeTextView: TextView = itemView.findViewById(R.id.transactionTypeTextView) // Add this line
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchTransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_transaction_item, parent, false)
        return SearchTransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchTransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.walletNameTextView.text = transaction.walletName
        holder.descriptionTextView.text = transaction.description
        holder.timestampTextView.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(transaction.timestamp)
        val transactionType = when (transaction.transactionType) {
            "income" -> {
                holder.amountTextView.setTextColor(ContextCompat.getColor(holder.itemView.context,
                    R.color.Income
                ))
                holder.transactionTypeTextView.setTextColor(ContextCompat.getColor(holder.itemView.context,
                    R.color.Income
                ))
                "Income"
            }
            "expense" -> {
                holder.amountTextView.setTextColor(ContextCompat.getColor(holder.itemView.context,
                    R.color.Expense
                ))
                holder.transactionTypeTextView.setTextColor(ContextCompat.getColor(holder.itemView.context,
                    R.color.Expense
                ))
                "Expense"
            }
            else -> ""
        }

        holder.amountTextView.text = transaction.amount.toString()
        holder.transactionTypeTextView.text = transactionType

    }

    override fun getItemCount() = transactions.size
}