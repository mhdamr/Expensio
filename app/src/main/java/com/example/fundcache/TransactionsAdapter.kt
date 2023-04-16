package com.example.fundcache

import android.content.Context
import android.graphics.Color
import android.graphics.Color.green
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fundcache.TransactionItem
import com.example.fundcache.R
import com.example.fundcache.databinding.TransactionItemBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionsAdapter(private val context: Context) : ListAdapter<TransactionItem, TransactionsAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    inner class TransactionViewHolder(val binding: TransactionItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        return TransactionViewHolder(TransactionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        with(holder.binding) {
            transactionType.text = transaction.type.capitalize(Locale.getDefault())
            transactionDescription.text = transaction.description
            transactionAmount.text = String.format("%.2f", transaction.amount)
            transactionDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(transaction.timestamp)

            if (transaction.type == "expense") {
                transactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            } else {
                transactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionItem>() {
        override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
            return oldItem == newItem
        }
    }
}