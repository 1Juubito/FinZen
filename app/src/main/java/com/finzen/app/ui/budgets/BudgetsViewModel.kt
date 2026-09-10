package com.finzen.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.BudgetEntity
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.model.BudgetProgress
import com.finzen.app.data.model.CategoryType
import com.finzen.app.data.repository.BudgetRepository
import com.finzen.app.data.repository.CategoryRepository
import com.finzen.app.util.MonthRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetsUiState(
    val monthRef: MonthRef = MonthRef.now(),
    val items: List<BudgetProgress> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val month = MutableStateFlow(MonthRef.now())

    private val progress = month.flatMapLatest { ref ->
        budgetRepository.observeProgress(ref.year, ref.month)
    }

    private val expenseCategories = categoryRepository.observeByType(CategoryType.EXPENSE)

    val uiState: StateFlow<BudgetsUiState> =
        combine(month, progress, expenseCategories) { ref, items, categories ->
            BudgetsUiState(
                monthRef = ref,
                items = items,
                expenseCategories = categories,
                totalBudget = items.sumOf { it.budget },
                totalSpent = items.sumOf { it.spent },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    fun previousMonth() { month.value = month.value.previous() }
    fun nextMonth() { month.value = month.value.next() }

    fun saveBudget(categoryId: Long, amount: Double) {
        viewModelScope.launch {
            val ref = month.value
            val existing = budgetRepository.getForCategory(categoryId, ref.year, ref.month)
            budgetRepository.upsert(
                BudgetEntity(
                    id = existing?.id ?: 0,
                    categoryId = categoryId,
                    amount = amount,
                    year = ref.year,
                    month = ref.month,
                )
            )
        }
    }

    fun deleteBudget(categoryId: Long) {
        viewModelScope.launch {
            val ref = month.value
            budgetRepository.getForCategory(categoryId, ref.year, ref.month)?.let {
                budgetRepository.delete(it)
            }
        }
    }
}
