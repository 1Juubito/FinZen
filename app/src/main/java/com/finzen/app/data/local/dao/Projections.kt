package com.finzen.app.data.local.dao

import androidx.room.Embedded
import com.finzen.app.data.local.entity.TransactionEntity

data class TransactionDetails(
    @Embedded val transaction: TransactionEntity,
    val categoryName: String?,
    val categoryColor: String?,
    val categoryIcon: String?,
    val accountName: String?,
    val accountColor: String?,
    val accountIcon: String?,
    val toAccountName: String?,
    val cardName: String?,
    val cardColor: String?,
)

data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String,
    val total: Double,
)

data class AccountDelta(
    val accountId: Long,
    val delta: Double,
)

data class CardSpent(
    val creditCardId: Long,
    val total: Double,
)

data class AmountPoint(
    val date: Long,
    val amount: Double,
)

data class CardInvoiceTotal(
    val creditCardId: Long,
    val invoiceDate: Long,
    val total: Double,
)
