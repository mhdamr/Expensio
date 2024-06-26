package com.example.expensio.Transactions

import java.util.*

data class SearchedTransactionItem(
    val walletId: String,
    val walletName: String,
    val description: String,
    val amount: Double,
    val timestamp: Date,
    val transactionType: String
)