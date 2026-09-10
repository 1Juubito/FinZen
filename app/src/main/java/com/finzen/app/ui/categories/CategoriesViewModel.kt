package com.finzen.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.model.CategoryType
import com.finzen.app.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CategoriesUiState(
    val expense: List<CategoryEntity> = emptyList(),
    val income: List<CategoryEntity> = emptyList(),
)

class CategoriesViewModel(
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = combine(
        categoryRepository.observeByType(CategoryType.EXPENSE),
        categoryRepository.observeByType(CategoryType.INCOME),
    ) { expense, income ->
        CategoriesUiState(
            expense = expense,
            income = income,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())
}
