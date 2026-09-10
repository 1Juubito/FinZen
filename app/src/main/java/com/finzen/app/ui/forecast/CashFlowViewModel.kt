package com.finzen.app.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.forecast.CashFlowCalculator
import com.finzen.app.data.forecast.ForecastInputs
import com.finzen.app.data.repository.AccountRepository
import com.finzen.app.data.repository.RecurringTransactionRepository
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class CashFlowMonthUi(
    val label: String,
    val income: Double,
    val expense: Double,
    val endBalance: Double,
    val isCurrent: Boolean,
)

data class CashFlowUiState(
    val startBalance: Double = 0.0,
    val months: List<CashFlowMonthUi> = emptyList(),
    val endBalance: Double = 0.0,
    val lowestBalance: Double = 0.0,
    val lowestLabel: String = "",
    val hasShortfall: Boolean = false,
    val loading: Boolean = true,
) {
    val isEmpty: Boolean get() = months.isEmpty()
}

class CashFlowViewModel(
    accountRepository: AccountRepository,
    recurringTransactionRepository: RecurringTransactionRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<CashFlowUiState> = combine(
        accountRepository.observeTotalBalance(),
        recurringTransactionRepository.observeActiveForTotals(),
        transactionRepository.observePendingDetails(),
        transactionRepository.observeOpenCardInvoiceTotals(),
    ) { startBalance, recurring, pending, cardInvoiceTotals ->
        val today = LocalDate.now()

        val recurInputs = ForecastInputs.recurring(recurring)
        val pendingInputs = ForecastInputs.pending(pending)
        val cardInputs = ForecastInputs.cardInvoices(cardInvoiceTotals)

        val projection = CashFlowCalculator.project(
            startBalance = startBalance,
            today = today,
            horizonMonths = HORIZON_MONTHS,
            recurring = recurInputs,
            pending = pendingInputs,
            cardInvoices = cardInputs,
        )

        val currentYm = YearMonth.from(today)
        val months = projection.map { p ->
            CashFlowMonthUi(
                label = DateUtils.shortMonthLabel(p.yearMonth.year, p.yearMonth.monthValue),
                income = p.income,
                expense = p.expense,
                endBalance = p.endBalance,
                isCurrent = p.yearMonth == currentYm,
            )
        }
        val lowest = projection.minByOrNull { it.endBalance }
        CashFlowUiState(
            startBalance = startBalance,
            months = months,
            endBalance = projection.lastOrNull()?.endBalance ?: startBalance,
            lowestBalance = lowest?.endBalance ?: startBalance,
            lowestLabel = lowest?.let { DateUtils.monthYearLabel(it.yearMonth.year, it.yearMonth.monthValue) }.orEmpty(),
            hasShortfall = projection.any { it.endBalance < 0.0 },
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashFlowUiState())

    companion object {
        const val HORIZON_MONTHS = 6
    }
}
