package com.finzen.app.data.repository

import com.finzen.app.data.local.dao.BudgetDao
import com.finzen.app.data.local.dao.CategoryDao
import com.finzen.app.data.local.dao.TransactionDao
import com.finzen.app.data.local.entity.BudgetEntity
import com.finzen.app.data.model.BudgetProgress
import com.finzen.app.data.model.CategoryType
import com.finzen.app.data.model.TransactionType
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
) {
    fun observeProgress(year: Int, month: Int): Flow<List<BudgetProgress>> {
        val (start, end) = DateUtils.monthBounds(year, month)
        return combine(
            budgetDao.observeForMonth(year, month),
            transactionDao.observeSpendingByCategory(TransactionType.EXPENSE, start, end),
            categoryDao.observeByType(CategoryType.EXPENSE),
        ) { budgets, spending, categories ->
            val spentMap = spending.associate { it.categoryId to it.total }
            val categoryMap = categories.associateBy { it.id }
            budgets.mapNotNull { budget ->
                val category = categoryMap[budget.categoryId] ?: return@mapNotNull null
                BudgetProgress(category, budget.amount, spentMap[budget.categoryId] ?: 0.0)
            }.sortedByDescending { it.fraction }
        }
    }

    suspend fun getForCategory(categoryId: Long, year: Int, month: Int): BudgetEntity? =
        budgetDao.getForCategory(categoryId, year, month)

    suspend fun upsert(budget: BudgetEntity) = budgetDao.upsert(budget)
    suspend fun delete(budget: BudgetEntity) = budgetDao.delete(budget)
}
