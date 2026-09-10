package com.finzen.app.data.forecast

import com.finzen.app.data.model.RecurrenceFrequency
import com.finzen.app.data.recurrence.RecurrenceScheduler
import java.time.LocalDate
import java.time.YearMonth

data class ForecastRecurring(
    val amount: Double,
    val isIncome: Boolean,
    val frequency: RecurrenceFrequency,
    val interval: Int,
    val nextDueDate: LocalDate,
    val endDate: LocalDate?,
)

data class ForecastFlow(
    val amount: Double,
    val isIncome: Boolean,
    val date: LocalDate,
)

data class MonthProjection(
    val yearMonth: YearMonth,
    val income: Double,
    val expense: Double,
    val endBalance: Double,
)

object CashFlowCalculator {

    private const val MAX_OCCURRENCES = 800

    fun project(
        startBalance: Double,
        today: LocalDate,
        horizonMonths: Int,
        recurring: List<ForecastRecurring>,
        pending: List<ForecastFlow>,
        cardInvoices: List<ForecastFlow>,
    ): List<MonthProjection> {
        if (horizonMonths <= 0) return emptyList()
        val firstMonth = YearMonth.from(today)
        val lastMonth = firstMonth.plusMonths((horizonMonths - 1).toLong())
        val income = DoubleArray(horizonMonths)
        val expense = DoubleArray(horizonMonths)

        fun indexFor(date: LocalDate): Int? {
            val ym = YearMonth.from(date)
            return when {
                ym.isBefore(firstMonth) -> 0
                ym.isAfter(lastMonth) -> null
                else -> (ym.year - firstMonth.year) * 12 + (ym.monthValue - firstMonth.monthValue)
            }
        }

        fun add(date: LocalDate, amount: Double, isIncome: Boolean) {
            val idx = indexFor(date) ?: return
            if (isIncome) income[idx] += amount else expense[idx] += amount
        }

        pending.forEach { add(it.date, it.amount, it.isIncome) }
        cardInvoices.forEach { add(it.date, it.amount, it.isIncome) }

        val firstDay = firstMonth.atDay(1)
        val lastDay = lastMonth.atEndOfMonth()
        recurring.forEach { r ->
            var occ = r.nextDueDate
            var guard = 0

            while (occ.isBefore(firstDay) && guard < MAX_OCCURRENCES) {
                occ = RecurrenceScheduler.advance(occ, r.frequency, r.interval)
                guard++
            }
            while (!occ.isAfter(lastDay) && guard < MAX_OCCURRENCES) {
                if (r.endDate != null && occ.isAfter(r.endDate)) break
                add(occ, r.amount, r.isIncome)
                occ = RecurrenceScheduler.advance(occ, r.frequency, r.interval)
                guard++
            }
        }

        var running = startBalance
        return (0 until horizonMonths).map { i ->
            running += income[i] - expense[i]
            MonthProjection(firstMonth.plusMonths(i.toLong()), income[i], expense[i], running)
        }
    }
}
