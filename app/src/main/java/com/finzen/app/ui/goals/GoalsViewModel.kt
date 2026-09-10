package com.finzen.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.GoalEntity
import com.finzen.app.data.repository.GoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalsUiState(
    val goals: List<GoalEntity> = emptyList(),
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0,
)

class GoalsViewModel(
    private val goalRepository: GoalRepository,
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = goalRepository.observeAll()
        .map { goals ->
            GoalsUiState(
                goals = goals,
                totalSaved = goals.sumOf { it.savedAmount },
                totalTarget = goals.sumOf { it.targetAmount },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun addFunds(id: Long, amount: Double) {
        viewModelScope.launch { goalRepository.addFunds(id, amount) }
    }

    fun delete(goal: GoalEntity) {
        viewModelScope.launch { goalRepository.delete(goal) }
    }
}
