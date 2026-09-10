package com.finzen.app.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.PersonalDebtEntity
import com.finzen.app.data.model.DebtDirection
import com.finzen.app.data.repository.PersonalDebtRepository
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class ReceivableUi(
    val id: Long,
    val person: String,
    val amount: Double,
    val dueLabel: String,
    val overdue: Boolean,
    val notes: String?,
)

data class InflowBucketUi(val label: String, val amount: Double)

data class ReceivablesUiState(
    val total: Double = 0.0,
    val overdueTotal: Double = 0.0,
    val upcomingTotal: Double = 0.0,
    val items: List<ReceivableUi> = emptyList(),
    val buckets: List<InflowBucketUi> = emptyList(),
    val hasItems: Boolean = false,
    val loading: Boolean = true,
)

class ReceivablesViewModel(
    private val repository: PersonalDebtRepository,
) : ViewModel() {

    val uiState: StateFlow<ReceivablesUiState> = repository.observeAll().map { all ->
        val today = LocalDate.now()
        val open = all.filter { it.direction == DebtDirection.THEY_OWE_ME && !it.settled }

        val items = open.sortedBy { it.dueDate ?: Long.MAX_VALUE }.map { d ->
            val dd = d.dueDate
            val overdue: Boolean
            val dueLabel: String
            if (dd == null) {
                overdue = false
                dueLabel = "Sem data de vencimento"
            } else {
                val diff = DateUtils.toLocalDate(dd).toEpochDay() - today.toEpochDay()
                overdue = diff < 0L
                dueLabel = when {
                    diff < 0L -> { val n = -diff; "Atrasado há $n ${if (n == 1L) "dia" else "dias"}" }
                    diff == 0L -> "Vence hoje"
                    else -> "Vence em ${DateUtils.dayMonthLabel(dd)}"
                }
            }
            ReceivableUi(d.id, d.person, d.amount, dueLabel, overdue, d.notes)
        }

        val overdueTotal = items.filter { it.overdue }.sumOf { it.amount }
        val total = open.sumOf { it.amount }
        ReceivablesUiState(
            total = total,
            overdueTotal = overdueTotal,
            upcomingTotal = total - overdueTotal,
            items = items,
            buckets = buckets(open, today),
            hasItems = open.isNotEmpty(),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceivablesUiState())

    fun markReceived(id: Long) {
        viewModelScope.launch { repository.setSettled(id, true) }
    }

    private fun buckets(open: List<PersonalDebtEntity>, today: LocalDate): List<InflowBucketUi> {
        val todayEpoch = today.toEpochDay()
        var overdue = 0.0
        var noDate = 0.0
        val byMonth = mutableMapOf<YearMonth, Double>()
        for (d in open) {
            val dd = d.dueDate
            if (dd == null) {
                noDate += d.amount
            } else {
                val local = DateUtils.toLocalDate(dd)
                if (local.toEpochDay() < todayEpoch) {
                    overdue += d.amount
                } else {
                    val ym = YearMonth.from(local)
                    byMonth[ym] = (byMonth[ym] ?: 0.0) + d.amount
                }
            }
        }
        return buildList {
            if (overdue > 0.0) add(InflowBucketUi("Atrasado", overdue))
            byMonth.entries.sortedBy { it.key }.forEach { (ym, amount) ->
                add(InflowBucketUi(monthLabel(ym, today), amount))
            }
            if (noDate > 0.0) add(InflowBucketUi("Sem data", noDate))
        }
    }

    private fun monthLabel(ym: YearMonth, today: LocalDate): String {
        val short = DateUtils.shortMonthLabel(ym.year, ym.monthValue)
        return if (ym.year == today.year) short else "$short/${ym.year % 100}"
    }
}
