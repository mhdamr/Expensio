package com.example.expensio

import com.google.firebase.Timestamp


data class RecurrenceTransaction(
    var id: String = "",
    var amount: Double = 0.0,
    var description: String = "",
    var type: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    var recurrence: String = ""
)