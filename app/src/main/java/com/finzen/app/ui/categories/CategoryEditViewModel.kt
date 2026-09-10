package com.finzen.app.ui.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.model.CategoryType
import com.finzen.app.data.repository.CategoryRepository
import com.finzen.app.ui.icons.PaletteColors
import com.finzen.app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryEditUiState(
    val name: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val colorHex: String = PaletteColors.swatches.first(),
    val iconKey: String = "other",
    val isDefault: Boolean = false,
    val isEditing: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

class CategoryEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val categoryId: Long = savedStateHandle.get<Long>(Routes.ARG_CATEGORY_ID) ?: -1L
    private val typeArg: String? = savedStateHandle.get<String>(Routes.ARG_CATEGORY_TYPE)
    private val initialType: CategoryType =
        if (typeArg == "INCOME") CategoryType.INCOME else CategoryType.EXPENSE

    private val _state = MutableStateFlow(
        CategoryEditUiState(
            type = initialType,
            isEditing = categoryId > 0L,
        )
    )
    val state = _state.asStateFlow()

    init {
        if (categoryId > 0L) loadExisting()
    }

    private fun loadExisting() {
        viewModelScope.launch {
            val category = categoryRepository.getById(categoryId) ?: return@launch
            _state.update {
                it.copy(
                    name = category.name,
                    type = category.type,
                    colorHex = category.colorHex,
                    iconKey = category.iconKey,
                    isDefault = category.isDefault,
                    isEditing = true,
                )
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun selectColor(hex: String) = _state.update { it.copy(colorHex = hex) }

    fun selectIcon(key: String) = _state.update { it.copy(iconKey = key) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val entity = CategoryEntity(
                id = if (s.isEditing) categoryId else 0L,
                name = s.name.trim(),
                type = s.type,
                colorHex = s.colorHex,
                iconKey = s.iconKey,
                isDefault = s.isDefault,
            )
            if (s.isEditing) {
                categoryRepository.update(entity)
            } else {
                categoryRepository.insert(entity)
            }
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        if (categoryId <= 0L) return
        viewModelScope.launch {
            categoryRepository.getById(categoryId)?.let { categoryRepository.delete(it) }
            _state.update { it.copy(saved = true) }
        }
    }
}
