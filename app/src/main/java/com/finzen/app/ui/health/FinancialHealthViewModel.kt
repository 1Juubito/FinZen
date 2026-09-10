package com.finzen.app.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.recurrence.RecurrenceScheduler
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

enum class HealthLevel { GOOD, OK, BAD }

data class FinancialHealthUiState(
    val hasData: Boolean = false,
    val hasIncome: Boolean = false,
    val hasExpense: Boolean = false,
    val savingsRate: Double = 0.0,
    val savingsLevel: HealthLevel = HealthLevel.OK,
    val reserveMonths: Double = 0.0,
    val reserveLevel: HealthLevel = HealthLevel.OK,
    val reserveReliable: Boolean = false,
    val commitment: Double = 0.0,
    val commitmentLevel: HealthLevel = HealthLevel.OK,
    val avgMonthlyIncome: Double = 0.0,
    val avgMonthlyExpense: Double = 0.0,
    val monthlySurplus: Double = 0.0,
    val fixedMonthly: Double = 0.0,
    val overall: HealthLevel = HealthLevel.OK,
    val loading: Boolean = true,
)

class FinancialHealthViewModel(
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    recurringTransactionRepository: RecurringTransactionRepository,
) : ViewModel() {

    private val ym = YearMonth.from(LocalDate.now())
    private val thisMonthStart = DateUtils.monthBounds(ym.year, ym.monthValue).first
    private val windowYm = ym.minusMonths(MONTHS.toLong())
    private val windowStart = DateUtils.monthBounds(windowYm.year, windowYm.monthValue).first

    val uiState: StateFlow<FinancialHealthUiState> = combine(
        transactionRepository.observeTotalByType(TransactionType.INCOME, windowStart, thisMonthStart),
        transactionRepository.observeTotalByType(TransactionType.EXPENSE, windowStart, thisMonthStart),
        accountRepository.observeTotalBalance(),
        recurringTransactionRepository.observeActiveForTotals(),
    ) { income, expense, balance, recurring ->
        val avgIncome = income / MONTHS
        val avgExpense = expense / MONTHS
        val fixedMonthly = recurring
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount * RecurrenceScheduler.occurrencesPerMonth(it.frequency, it.interval) }

        val hasIncome = income > 0.0
        val hasExpense = expense > 0.0
        val savingsRate = if (hasIncome) (income - expense) / income else 0.0
        val reserveMonths = if (avgExpense > 0.0) balance / avgExpense else 0.0
        val commitment = if (avgIncome > 0.0) fixedMonthly / avgIncome else 0.0

        val reserveReliable = hasExpense && (!hasIncome || avgExpense >= avgIncome * 0.10)

        val savingsLevel = when {
            savingsRate >= 0.20 -> HealthLevel.GOOD
            savingsRate >= 0.05 -> HealthLevel.OK
            else -> HealthLevel.BAD
        }
        val reserveLevel = when {
            reserveMonths >= 6.0 -> HealthLevel.GOOD
            reserveMonths >= 3.0 -> HealthLevel.OK
            else -> HealthLevel.BAD
        }
        val commitmentLevel = when {
            commitment <= 0.30 -> HealthLevel.GOOD
            commitment <= 0.50 -> HealthLevel.OK
            else -> HealthLevel.BAD
        }

        val levels = buildList {
            if (hasIncome) add(savingsLevel)
            if (reserveReliable) add(reserveLevel)
            if (hasIncome) add(commitmentLevel)
        }
        val overall = when {
            levels.isEmpty() -> HealthLevel.OK
            levels.count { it == HealthLevel.BAD } >= 2 -> HealthLevel.BAD
            levels.all { it == HealthLevel.GOOD } -> HealthLevel.GOOD
            else -> HealthLevel.OK
        }

        FinancialHealthUiState(
            hasData = hasIncome || hasExpense,
            hasIncome = hasIncome,
            hasExpense = hasExpense,
            savingsRate = savingsRate,
            savingsLevel = savingsLevel,
            reserveMonths = reserveMonths,
            reserveLevel = reserveLevel,
            reserveReliable = reserveReliable,
            commitment = commitment,
            commitmentLevel = commitmentLevel,
            avgMonthlyIncome = avgIncome,
            avgMonthlyExpense = avgExpense,
            monthlySurplus = avgIncome - avgExpense,
            fixedMonthly = fixedMonthly,
            overall = overall,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinancialHealthUiState())

    companion object {

        const val MONTHS = 3
    }
}
