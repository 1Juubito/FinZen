package com.finzen.app.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.GoalEntity
import com.finzen.app.data.repository.GoalRepository
import com.finzen.app.ui.icons.PaletteColors
import com.finzen.app.ui.navigation.Routes
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalEditUiState(
    val name: String = "",
    val targetDigits: String = "",
    val savedAmount: Double = 0.0,
    val deadline: Long? = null,
    val colorHex: String = PaletteColors.swatches.first(),
    val iconKey: String = "target",
    val isEditing: Boolean = false,
    val saved: Boolean = false,
) {
    val targetAmount: Double get() = Money.centsToValue(targetDigits)
    val targetFormatted: String get() = Money.formatCentsInput(targetDigits)
    val deadlineLabel: String get() = deadline?.let { DateUtils.fullDateLabel(it) } ?: "Sem prazo"
    val canSave: Boolean get() = name.isNotBlank() && targetAmount > 0.0
}

class GoalEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val goalId: Long = savedStateHandle.get<Long>(Routes.ARG_GOAL_ID) ?: -1L

    private var createdAt: Long = DateUtils.now()

    private val _state = MutableStateFlow(GoalEditUiState(isEditing = goalId > 0L))
    val state = _state.asStateFlow()

    init {
        if (goalId > 0L) loadExisting()
    }

    private fun loadExisting() {
        viewModelScope.launch {

            val goal = goalRepository.observeById(goalId).first() ?: return@launch
            createdAt = goal.createdAt
            _state.update {
                it.copy(
                    name = goal.name,
                    targetDigits = toDigits(goal.targetAmount),
                    savedAmount = goal.savedAmount,
                    deadline = goal.deadline,
                    colorHex = goal.colorHex,
                    iconKey = goal.iconKey,
                    isEditing = true,
                )
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun appendDigit(digit: Char) {
        if (!digit.isDigit()) return
        _state.update {
            val next = (it.targetDigits + digit).trimStart('0').take(12)
            it.copy(targetDigits = next)
        }
    }

    fun backspace() {
        _state.update { it.copy(targetDigits = it.targetDigits.dropLast(1)) }
    }

    fun setDeadline(millis: Long?) = _state.update { it.copy(deadline = millis) }

    fun clearDeadline() = _state.update { it.copy(deadline = null) }

    fun selectColor(hex: String) = _state.update { it.copy(colorHex = hex) }

    fun selectIcon(key: String) = _state.update { it.copy(iconKey = key) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val entity = GoalEntity(
                id = if (s.isEditing) goalId else 0L,
                name = s.name.trim(),
                targetAmount = s.targetAmount,
                savedAmount = s.savedAmount,
                deadline = s.deadline,
                colorHex = s.colorHex,
                iconKey = s.iconKey,
                createdAt = createdAt,
            )
            goalRepository.upsert(entity)
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val s = _state.value
        if (goalId <= 0L) return
        viewModelScope.launch {
            val entity = GoalEntity(
                id = goalId,
                name = s.name.trim(),
                targetAmount = s.targetAmount,
                savedAmount = s.savedAmount,
                deadline = s.deadline,
                colorHex = s.colorHex,
                iconKey = s.iconKey,
                createdAt = createdAt,
            )
            goalRepository.delete(entity)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun toDigits(amount: Double): String {
        val cents = Math.round(amount * 100.0)
        return if (cents <= 0) "" else cents.toString()
    }
}
