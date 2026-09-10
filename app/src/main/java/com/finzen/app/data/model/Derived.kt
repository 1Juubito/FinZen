package com.finzen.app.data.model

import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.local.entity.CreditCardEntity

data class AccountWithBalance(
    val account: AccountEntity,
    val balance: Double,
)

data class CardWithUsage(
    val card: CreditCardEntity,
    val used: Double,
    val currentInvoice: Double = 0.0,
) {
    val available: Double get() = (card.creditLimit - used).coerceAtLeast(0.0)
    val usedFraction: Float
        get() = if (card.creditLimit <= 0) 0f else (used / card.creditLimit).toFloat().coerceIn(0f, 1f)
}

data class BudgetProgress(
    val category: CategoryEntity,
    val budget: Double,
    val spent: Double,
) {
    val remaining: Double get() = budget - spent
    val fraction: Float
        get() = if (budget <= 0) 0f else (spent / budget).toFloat().coerceIn(0f, 1f)
    val isOver: Boolean get() = spent > budget
}
