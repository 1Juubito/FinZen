package com.finzen.app.ui.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.local.entity.InvestmentEntity
import com.finzen.app.data.local.entity.TransactionEntity
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.repository.AccountRepository
import com.finzen.app.data.repository.InvestmentRepository
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AllocationSlice(
    val label: String,
    val colorHex: String,
    val total: Double,
    val fraction: Float,
)

data class InvestmentsUiState(
    val investments: List<InvestmentEntity> = emptyList(),
    val totalInvested: Double = 0.0,
    val totalCurrent: Double = 0.0,
    val allocation: List<AllocationSlice> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
) {
    val totalProfit: Double get() = totalCurrent - totalInvested
    val profitability: Double get() = if (totalInvested > 0.0) totalProfit / totalInvested else 0.0
    val isEmpty: Boolean get() = investments.isEmpty()
}

class InvestmentsViewModel(
    private val investmentRepository: InvestmentRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<InvestmentsUiState> = combine(
        investmentRepository.observeAll(),
        accountRepository.observeActive(),
    ) { list, accounts ->
            val totalCurrent = list.sumOf { it.currentValue }
            val totalInvested = list.sumOf { it.investedAmount }
            val allocation = list
                .groupBy { it.type }
                .map { (type, items) ->
                    val total = items.sumOf { it.currentValue }
                    AllocationSlice(
                        label = type.label,
                        colorHex = type.colorHex,
                        total = total,
                        fraction = if (totalCurrent > 0.0) (total / totalCurrent).toFloat() else 0f,
                    )
                }
                .filter { it.total > 0.0 }
                .sortedByDescending { it.total }
            InvestmentsUiState(
                investments = list,
                totalInvested = totalInvested,
                totalCurrent = totalCurrent,
                allocation = allocation,
                accounts = accounts,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvestmentsUiState())

    fun contribute(id: Long, amount: Double, sourceAccountId: Long?, investmentName: String) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            investmentRepository.contribute(id, amount)
            if (sourceAccountId != null) {
                transactionRepository.insert(
                    TransactionEntity(
                        description = "Aporte • $investmentName",
                        amount = amount,
                        type = TransactionType.TRANSFER,
                        date = DateUtils.now(),
                        accountId = sourceAccountId,
                        toAccountId = null,
                        isPaid = true,
                    ),
                )
            }
        }
    }

    fun updateValue(id: Long, value: Double) {
        viewModelScope.launch { investmentRepository.updateValue(id, value) }
    }

    fun addYield(id: Long, amount: Double) {
        if (amount <= 0.0) return
        viewModelScope.launch { investmentRepository.addYield(id, amount) }
    }

    fun delete(investment: InvestmentEntity) {
        viewModelScope.launch { investmentRepository.delete(investment) }
    }
}
