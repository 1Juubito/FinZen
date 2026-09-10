package com.finzen.app.ui.spendable

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

data class SpendableUiState(
    val balanceToday: Double = 0.0,
    val incoming: Double = 0.0,
    val outgoing: Double = 0.0,
    val spendable: Double = 0.0,
    val perDay: Double = 0.0,
    val daysLeft: Int = 0,
    val monthLabel: String = "",
    val loading: Boolean = true,
) {
    val negative: Boolean get() = spendable < 0.0
}

class SpendableViewModel(
    accountRepository: AccountRepository,
    recurringTransactionRepository: RecurringTransactionRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<SpendableUiState> = combine(
        accountRepository.observeTotalBalance(),
        recurringTransactionRepository.observeActiveForTotals(),
        transactionRepository.observePendingDetails(),
        transactionRepository.observeOpenCardInvoiceTotals(),
    ) { startBalance, recurring, pending, cardInvoiceTotals ->
        val today = LocalDate.now()

        val month = CashFlowCalculator.project(
            startBalance = startBalance,
            today = today,
            horizonMonths = 1,
            recurring = ForecastInputs.recurring(recurring),
            pending = ForecastInputs.pending(pending),
            cardInvoices = ForecastInputs.cardInvoices(cardInvoiceTotals),
        ).first()

        val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
        val ym = YearMonth.from(today)
        SpendableUiState(
            balanceToday = startBalance,
            incoming = month.income,
            outgoing = month.expense,
            spendable = month.endBalance,
            perDay = if (daysLeft > 0) month.endBalance / daysLeft else month.endBalance,
            daysLeft = daysLeft,
            monthLabel = DateUtils.monthYearLabel(ym.year, ym.monthValue),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpendableUiState())
}
