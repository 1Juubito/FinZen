package com.finzen.app.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.local.entity.CategoryRuleEntity
import com.finzen.app.data.repository.CategoryRepository
import com.finzen.app.data.repository.CategoryRuleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryRuleItemUi(
    val entity: CategoryRuleEntity,
    val categoryName: String?,
    val categoryColor: String?,
    val categoryIcon: String?,
)

data class CategoryRulesUiState(
    val rules: List<CategoryRuleItemUi> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
)

class CategoryRulesViewModel(
    private val ruleRepository: CategoryRuleRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<CategoryRulesUiState> = combine(
        ruleRepository.observeAll(),
        categoryRepository.observeAll(),
    ) { rules, categories ->
        CategoryRulesUiState(
            rules = rules.map { rule ->
                val category = categories.firstOrNull { it.id == rule.categoryId }
                CategoryRuleItemUi(rule, category?.name, category?.colorHex, category?.iconKey)
            },
            categories = categories.sortedBy { it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryRulesUiState())

    fun addRule(keyword: String, categoryId: Long) {
        val cleaned = keyword.trim()
        if (cleaned.isEmpty() || categoryId <= 0L) return
        viewModelScope.launch {
            ruleRepository.insert(CategoryRuleEntity(keyword = cleaned, categoryId = categoryId))
        }
    }

    fun delete(rule: CategoryRuleEntity) {
        viewModelScope.launch { ruleRepository.delete(rule) }
    }
}
