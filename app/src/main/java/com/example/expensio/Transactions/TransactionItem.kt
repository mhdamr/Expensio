package com.example.expensio.Transactions

import java.util.*

data class TransactionItem(
    var id: String = "",
    var amount: Double = 0.0,
    var description: String = "",
    var type: String = "",
    var timestamp: Date = Date()
)