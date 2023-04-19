package com.example.fundcache

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fundcache.databinding.DateHeaderBinding
import com.example.fundcache.databinding.TransactionItemBinding
import java.text.SimpleDateFormat
import java.util.*

sealed class TransactionListItem {
    data class DateHeader(val date: String) : TransactionListItem()
    data class Transaction(val transaction: TransactionItem) : TransactionListItem()
}

class TransactionsAdapter(private val context: Context) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(TransactionDiffCallback()) {

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
                        transactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
                    } else {
                        transactionAmount.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
                    }
                }
            }
        }
    }

    inner class DateHeaderViewHolder(val binding: DateHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    inner class TransactionViewHolder(val binding: TransactionItemBinding) : RecyclerView.ViewHolder(binding.root)

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