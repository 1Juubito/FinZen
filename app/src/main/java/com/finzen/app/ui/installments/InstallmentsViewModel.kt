package com.finzen.app.ui.installments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.dao.TransactionDetails
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class InstallmentMonth(
    val label: String,
    val total: Double,
    val items: List<TransactionDetails>,
)

data class InstallmentsUiState(
    val months: List<InstallmentMonth> = emptyList(),
    val grandTotal: Double = 0.0,
    val loading: Boolean = true,
)

class InstallmentsViewModel(
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val from = DateUtils.localDateToMillis(YearMonth.now().atDay(1))

    val uiState: StateFlow<InstallmentsUiState> =
        transactionRepository.observeFutureInstallments(from).map { list ->
            val months = list
                .groupBy { YearMonth.from(DateUtils.toLocalDate(it.transaction.invoiceDate)) }
                .toSortedMap()
                .map { (ym, items) ->
                    InstallmentMonth(
                        label = DateUtils.monthYearLabel(ym.year, ym.monthValue),
                        total = items.sumOf { it.transaction.amount },
                        items = items,
                    )
                }
            InstallmentsUiState(
                months = months,
                grandTotal = list.sumOf { it.transaction.amount },
                loading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstallmentsUiState())
}
