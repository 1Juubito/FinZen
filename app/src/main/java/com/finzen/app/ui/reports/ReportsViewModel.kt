package com.finzen.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.dao.AmountPoint
import com.finzen.app.data.local.dao.CategorySpending
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.ui.components.LinePoint
import com.finzen.app.ui.components.MonthBarData
import com.finzen.app.util.DateUtils
import com.finzen.app.util.MonthRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

data class CategorySlice(
    val name: String,
    val colorHex: String,
    val total: Double,
    val fraction: Float,
)

data class CategoryComparison(
    val categoryId: Long,
    val name: String,
    val colorHex: String,
    val current: Double,
    val previous: Double,
) {
    val delta: Double get() = current - previous

    val isNew: Boolean get() = previous <= 0.0 && current > 0.0

    val pctChange: Float? get() = if (previous <= 0.0) null else ((current - previous) / previous).toFloat()
}

enum class FrequencyPeriod(val days: Long, val label: String) {
    WEEK(7, "7 dias"),
    MONTH(30, "30 dias"),
    YEAR(365, "Último ano"),
}

private data class FrequencyData(
    val period: FrequencyPeriod,
    val points: List<LinePoint>,
    val total: Double,
)

private data class BreakdownData(
    val current: List<CategorySpending>,
    val comparison: List<CategoryComparison>,
)

data class ReportsUiState(
    val monthRef: MonthRef = MonthRef.now(),
    val type: TransactionType = TransactionType.EXPENSE,
    val slices: List<CategorySlice> = emptyList(),
    val comparisons: List<CategoryComparison> = emptyList(),
    val total: Double = 0.0,
    val months: List<MonthBarData> = emptyList(),
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val frequencyPeriod: FrequencyPeriod = FrequencyPeriod.WEEK,
    val frequencyPoints: List<LinePoint> = emptyList(),
    val frequencyTotal: Double = 0.0,
) {
    val monthBalance: Double get() = monthIncome - monthExpense
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val month = MutableStateFlow(MonthRef.now())
    private val selectedType = MutableStateFlow(TransactionType.EXPENSE)
    private val frequencyPeriod = MutableStateFlow(FrequencyPeriod.WEEK)

    private val breakdownFlow: Flow<BreakdownData> =
        combine(month, selectedType) { m, type -> m to type }
            .flatMapLatest { (m, type) ->
                val (start, end) = m.bounds
                val (prevStart, prevEnd) = m.previous().bounds
                combine(
                    transactionRepository.observeSpendingByCategory(type, start, end),
                    transactionRepository.observeSpendingByCategory(type, prevStart, prevEnd),
                ) { current, previous ->
                    BreakdownData(current, buildComparison(current, previous))
                }
            }

    private val sixMonthFlow: Flow<List<MonthBarData>> =
        month.flatMapLatest { current ->
            val monthFlows: List<Flow<MonthBarData>> = (5 downTo 0).map { k ->
                val ref = current.minusMonths(k)
                val (start, end) = ref.bounds
                combine(
                    transactionRepository.observeTotalByType(TransactionType.INCOME, start, end),
                    transactionRepository.observeTotalByType(TransactionType.EXPENSE, start, end),
                ) { income, expense ->
                    MonthBarData(ref.shortLabel, income.toFloat(), expense.toFloat())
                }
            }
            combine(monthFlows) { array -> array.toList() }
        }

    private val frequencyFlow: Flow<FrequencyData> = frequencyPeriod.flatMapLatest { period ->
        val today = LocalDate.now()
        if (period == FrequencyPeriod.YEAR) {
            val firstMonth = YearMonth.from(today).minusMonths(11)
            val start = DateUtils.localDateToMillis(firstMonth.atDay(1))
            val end = DateUtils.localDateToMillis(today.plusDays(1))
            transactionRepository.observeExpensePoints(start, end).map { points ->
                aggregateMonthly(points, today, period)
            }
        } else {
            val startDate = today.minusDays(period.days - 1)
            val start = DateUtils.localDateToMillis(startDate)
            val end = DateUtils.localDateToMillis(today.plusDays(1))
            transactionRepository.observeExpensePoints(start, end).map { points ->
                aggregateDaily(points, startDate, today, period)
            }
        }
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        month,
        selectedType,
        breakdownFlow,
        sixMonthFlow,
        frequencyFlow,
    ) { ref, type, breakdown, months, frequency ->
        val spending = breakdown.current
        val total = spending.sumOf { it.total }
        val slices = if (total <= 0.0) {
            emptyList()
        } else {
            spending.map {
                CategorySlice(
                    name = it.categoryName,
                    colorHex = it.categoryColor,
                    total = it.total,
                    fraction = (it.total / total).toFloat(),
                )
            }
        }
        val current = months.lastOrNull()
        ReportsUiState(
            monthRef = ref,
            type = type,
            slices = slices,
            comparisons = breakdown.comparison,
            total = total,
            months = months,
            monthIncome = current?.income?.toDouble() ?: 0.0,
            monthExpense = current?.expense?.toDouble() ?: 0.0,
            frequencyPeriod = frequency.period,
            frequencyPoints = frequency.points,
            frequencyTotal = frequency.total,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun previousMonth() { month.value = month.value.previous() }
    fun nextMonth() { month.value = month.value.next() }
    fun setType(type: TransactionType) { selectedType.value = type }
    fun setFrequencyPeriod(period: FrequencyPeriod) { frequencyPeriod.value = period }

    private fun buildComparison(
        current: List<CategorySpending>,
        previous: List<CategorySpending>,
    ): List<CategoryComparison> {
        val currentById = current.associateBy { it.categoryId }
        val previousById = previous.associateBy { it.categoryId }
        val ids = LinkedHashSet<Long>().apply {
            addAll(current.map { it.categoryId })
            addAll(previous.map { it.categoryId })
        }
        return ids.mapNotNull { id ->
            val meta = currentById[id] ?: previousById[id] ?: return@mapNotNull null
            val cur = currentById[id]?.total ?: 0.0
            val prev = previousById[id]?.total ?: 0.0
            if (cur <= 0.0 && prev <= 0.0) return@mapNotNull null
            CategoryComparison(id, meta.categoryName, meta.categoryColor, cur, prev)
        }.sortedByDescending { abs(it.delta) }
    }

    private fun aggregateDaily(
        points: List<AmountPoint>,
        startDate: LocalDate,
        today: LocalDate,
        period: FrequencyPeriod,
    ): FrequencyData {
        val sums = LinkedHashMap<LocalDate, Double>()
        var day = startDate
        while (!day.isAfter(today)) {
            sums[day] = 0.0
            day = day.plusDays(1)
        }
        points.forEach { p ->
            val date = DateUtils.toLocalDate(p.date)
            sums[date]?.let { sums[date] = it + p.amount }
        }
        val line = sums.entries.map { (date, totalForDay) ->
            LinePoint("%02d/%02d".format(date.dayOfMonth, date.monthValue), totalForDay.toFloat())
        }
        return FrequencyData(period, line, sums.values.sum())
    }

    private fun aggregateMonthly(
        points: List<AmountPoint>,
        today: LocalDate,
        period: FrequencyPeriod,
    ): FrequencyData {
        val sums = LinkedHashMap<YearMonth, Double>()
        var ym = YearMonth.from(today).minusMonths(11)
        repeat(12) {
            sums[ym] = 0.0
            ym = ym.plusMonths(1)
        }
        points.forEach { p ->
            val key = YearMonth.from(DateUtils.toLocalDate(p.date))
            sums[key]?.let { sums[key] = it + p.amount }
        }
        val line = sums.entries.map { (m, totalForMonth) ->
            LinePoint(DateUtils.shortMonthLabel(m.year, m.monthValue), totalForMonth.toFloat())
        }
        return FrequencyData(period, line, sums.values.sum())
    }
}
