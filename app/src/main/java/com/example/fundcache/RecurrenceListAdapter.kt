package com.example.fundcache

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

    class RecurrenceListAdapter(
        private var recurrenceTransactions: MutableList<RecurrenceTransaction>,
        private val onDeleteClick: (String) -> Unit
    ) : RecyclerView.Adapter<RecurrenceListAdapter.RecurrenceViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecurrenceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recurrence_item, parent, false)
            return RecurrenceViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecurrenceViewHolder, position: Int) {
            holder.bind(recurrenceTransactions[position], onDeleteClick)
        }

        override fun getItemCount() = recurrenceTransactions.size

        fun setRecurrenceTransactions(newRecurrenceTransactions: MutableList<RecurrenceTransaction>) {
            recurrenceTransactions = newRecurrenceTransactions
            notifyDataSetChanged()
        }

        class RecurrenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val description: TextView = itemView.findViewById(R.id.recurrence_description)
            private val deleteIcon: ImageView = itemView.findViewById(R.id.delete_recurrence_icon)

            fun bind(transaction: RecurrenceTransaction, onDeleteClick: (String) -> Unit) {
                description.text = transaction.description
                deleteIcon.setOnClickListener {
                    onDeleteClick(transaction.id)
                }
            }
        }
    }
