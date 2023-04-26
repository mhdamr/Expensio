package com.example.fundcache

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

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
            /*private val infoIcon: ImageView = itemView.findViewById(R.id.info_icon)*/
            private val description: TextView = itemView.findViewById(R.id.recurrence_description)
            /*private val calendarIcon: ImageView = itemView.findViewById(R.id.calendar_icon)*/
            private val timestamp: TextView = itemView.findViewById(R.id.recurrence_timestamp)
            private val amount: TextView = itemView.findViewById(R.id.recurrence_amount)
            private val deleteIcon: ImageView = itemView.findViewById(R.id.delete_recurrence_icon)

            fun bind(transaction: RecurrenceTransaction, onDeleteClick: (String) -> Unit) {
                description.text = transaction.description
                val date = transaction.timestamp.toDate() // convert Firebase Timestamp to Date
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) // specify date format
                timestamp.text = dateFormat.format(date) // set formatted date to TextView
                if (transaction.type == "expense"){
                    amount.setTextColor(Color.parseColor("#EE3434"))
                    amount.text = "-" + transaction.amount.toString()
                }else {
                    amount.setTextColor(Color.parseColor("#39FA41"))
                    amount.text = "+" + transaction.amount.toString()
                }
                deleteIcon.setOnClickListener {
                    onDeleteClick(transaction.id)
                }
            }
        }
    }
