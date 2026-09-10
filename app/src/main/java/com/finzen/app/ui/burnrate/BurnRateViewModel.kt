package com.finzen.app.ui.burnrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class BurnRateUiState(
    val monthLabel: String = "",
    val spentSoFar: Double = 0.0,
    val daysElapsed: Int = 0,
    val daysInMonth: Int = 0,
    val daysLeft: Int = 0,
    val dailyRate: Double = 0.0,
    val projected: Double = 0.0,
    val lastMonthTotal: Double = 0.0,
    val loading: Boolean = true,
) {
    val hasData: Boolean get() = spentSoFar > 0.0
    val hasComparison: Boolean get() = lastMonthTotal > 0.0

    val deltaPct: Double get() = if (lastMonthTotal > 0.0) (projected - lastMonthTotal) / lastMonthTotal * 100.0 else 0.0
    val aboveLastMonth: Boolean get() = projected > lastMonthTotal
    val monthProgress: Float get() = if (daysInMonth > 0) daysElapsed.toFloat() / daysInMonth else 0f

    val early: Boolean get() = daysElapsed in 1..3
}

class BurnRateViewModel(
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val today = LocalDate.now()
    private val ym = YearMonth.from(today)
    private val monthBounds = DateUtils.monthBounds(ym.year, ym.monthValue)
    private val lastMonth = ym.minusMonths(1)
    private val lastMonthBounds = DateUtils.monthBounds(lastMonth.year, lastMonth.monthValue)
    private val tomorrowStart = DateUtils.localDateToMillis(today.plusDays(1))

    val uiState: StateFlow<BurnRateUiState> = combine(

        transactionRepository.observeTotalByType(TransactionType.EXPENSE, monthBounds.first, tomorrowStart),
        transactionRepository.observeTotalByType(TransactionType.EXPENSE, lastMonthBounds.first, lastMonthBounds.second),
    ) { spentSoFar, lastMonthTotal ->
        val daysElapsed = today.dayOfMonth
        val daysInMonth = today.lengthOfMonth()
        val dailyRate = spentSoFar / daysElapsed
        BurnRateUiState(
            monthLabel = DateUtils.monthYearLabel(ym.year, ym.monthValue),
            spentSoFar = spentSoFar,
            daysElapsed = daysElapsed,
            daysInMonth = daysInMonth,
            daysLeft = daysInMonth - daysElapsed,
            dailyRate = dailyRate,
            projected = dailyRate * daysInMonth,
            lastMonthTotal = lastMonthTotal,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BurnRateUiState())
}
