package com.finzen.app.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.PersonalDebtEntity
import com.finzen.app.data.model.DebtDirection
import com.finzen.app.data.repository.PersonalDebtRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebtsUiState(
    val theyOwe: List<PersonalDebtEntity> = emptyList(),
    val iOwe: List<PersonalDebtEntity> = emptyList(),
    val settled: List<PersonalDebtEntity> = emptyList(),
    val theyOweTotal: Double = 0.0,
    val iOweTotal: Double = 0.0,
    val loading: Boolean = true,
) {
    val isEmpty: Boolean get() = theyOwe.isEmpty() && iOwe.isEmpty() && settled.isEmpty()
}

class DebtsViewModel(
    private val repository: PersonalDebtRepository,
) : ViewModel() {

    val uiState: StateFlow<DebtsUiState> = repository.observeAll().map { all ->
        val open = all.filterNot { it.settled }
        val theyOwe = open.filter { it.direction == DebtDirection.THEY_OWE_ME }
        val iOwe = open.filter { it.direction == DebtDirection.I_OWE }
        DebtsUiState(
            theyOwe = theyOwe,
            iOwe = iOwe,
            settled = all.filter { it.settled },
            theyOweTotal = theyOwe.sumOf { it.amount },
            iOweTotal = iOwe.sumOf { it.amount },
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtsUiState())

    fun save(debt: PersonalDebtEntity) {
        viewModelScope.launch { repository.upsert(debt) }
    }

    fun setSettled(id: Long, settled: Boolean) {
        viewModelScope.launch { repository.setSettled(id, settled) }
    }

    fun delete(debt: PersonalDebtEntity) {
        viewModelScope.launch { repository.delete(debt) }
    }
}
