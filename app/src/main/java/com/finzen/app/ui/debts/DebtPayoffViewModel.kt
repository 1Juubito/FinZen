package com.finzen.app.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.debt.DebtPayoffCalculator
import com.finzen.app.data.debt.PayoffDebt
import com.finzen.app.data.debt.PayoffInflow
import com.finzen.app.data.debt.PayoffStrategy
import com.finzen.app.data.model.DebtDirection
import com.finzen.app.data.repository.PersonalDebtRepository
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class PayoffStepUi(
    val id: Long,
    val person: String,
    val amount: Double,
    val position: Int,
    val dueLabel: String?,
    val clearedLabel: String?,
)

data class DebtPayoffUiState(
    val strategy: PayoffStrategy = PayoffStrategy.SNOWBALL,
    val budgetDigits: String = "",
    val monthlyBudget: Double = 0.0,
    val totalOwed: Double = 0.0,
    val receivableTotal: Double = 0.0,
    val netOwed: Double = 0.0,
    val includeReceivables: Boolean = false,
    val steps: List<PayoffStepUi> = emptyList(),
    val monthsToDebtFree: Int? = null,
    val debtFreeLabel: String = "",
    val hasDebts: Boolean = false,
    val loading: Boolean = true,
) {
    val hasBudget: Boolean get() = monthlyBudget > 0.0
    val hasReceivables: Boolean get() = receivableTotal > 0.0
}

class DebtPayoffViewModel(
    private val repository: PersonalDebtRepository,
) : ViewModel() {

    private val strategy = MutableStateFlow(PayoffStrategy.SNOWBALL)
    private val budgetDigits = MutableStateFlow("")
    private val includeReceivables = MutableStateFlow(false)

    val uiState: StateFlow<DebtPayoffUiState> = combine(
        repository.observeAll(),
        strategy,
        budgetDigits,
        includeReceivables,
    ) { all, strat, digits, includeRecv ->
        val now = YearMonth.now()
        val debts = all
            .filter { it.direction == DebtDirection.I_OWE && !it.settled }
            .map { PayoffDebt(it.id, it.person, it.amount, it.dueDate) }
        val receivables = all.filter { it.direction == DebtDirection.THEY_OWE_ME && !it.settled }
        val receivableTotal = receivables.sumOf { it.amount }
        val budget = Money.centsToValue(digits)
        val inflows = if (includeRecv) {

            receivables.filter { it.dueDate != null }
                .map { PayoffInflow(it.amount, monthOffset(now, it.dueDate)) }
        } else {
            emptyList()
        }
        val plan = DebtPayoffCalculator.plan(debts, strat, budget, inflows)

        DebtPayoffUiState(
            strategy = strat,
            budgetDigits = digits,
            monthlyBudget = budget,
            totalOwed = plan.totalOwed,
            receivableTotal = receivableTotal,
            netOwed = plan.totalOwed - receivableTotal,
            includeReceivables = includeRecv,
            steps = plan.steps.map { s ->
                PayoffStepUi(
                    id = s.debt.id,
                    person = s.debt.person,
                    amount = s.debt.amount,
                    position = s.position,
                    dueLabel = s.debt.dueDate?.let { "vence ${DateUtils.dayMonthLabel(it)}" },
                    clearedLabel = s.clearedMonthOffset?.let { clearedLabel(now, it) },
                )
            },
            monthsToDebtFree = plan.monthsToDebtFree,
            debtFreeLabel = plan.monthsToDebtFree?.let {
                val ym = now.plusMonths((it - 1).toLong())
                DateUtils.monthYearLabel(ym.year, ym.monthValue)
            }.orEmpty(),
            hasDebts = debts.isNotEmpty(),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtPayoffUiState())

    fun setStrategy(value: PayoffStrategy) { strategy.value = value }
    fun appendDigit(d: Char) { budgetDigits.value = (budgetDigits.value + d).trimStart('0').take(9) }
    fun backspace() { budgetDigits.value = budgetDigits.value.dropLast(1) }
    fun setBudgetDigits(digits: String) { budgetDigits.value = digits }
    fun setIncludeReceivables(value: Boolean) { includeReceivables.value = value }
    fun settle(id: Long) { viewModelScope.launch { repository.setSettled(id, true) } }

    private fun monthOffset(now: YearMonth, dueDate: Long?): Int {
        if (dueDate == null) return 0
        val ym = YearMonth.from(DateUtils.toLocalDate(dueDate))
        return ((ym.year - now.year) * 12 + (ym.monthValue - now.monthValue)).coerceAtLeast(0)
    }

    private fun clearedLabel(now: YearMonth, offset: Int): String {
        if (offset == 0) return "quita este mês"
        val ym = now.plusMonths(offset.toLong())
        return "quita em ${DateUtils.shortMonthLabel(ym.year, ym.monthValue)}/${ym.year % 100}"
    }
}
