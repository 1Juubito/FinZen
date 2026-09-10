package com.finzen.app.data.projection

import com.finzen.app.data.model.RecurrenceFrequency
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.recurrence.RecurrenceScheduler
import java.time.LocalDate

object ProjectionCalculator {

    data class RecurringPlan(
        val amount: Double,
        val type: TransactionType,
        val frequency: RecurrenceFrequency,
        val interval: Int,
        val nextDue: LocalDate,
        val end: LocalDate?,
        val isCard: Boolean,
        val active: Boolean,
    )

    data class Expected(val income: Double, val expense: Double)

    fun upcomingRecurrenceTotals(
        plans: List<RecurringPlan>,
        from: LocalDate,
        to: LocalDate,
    ): Expected {
        if (to.isBefore(from)) return Expected(0.0, 0.0)
        var income = 0.0
        var expense = 0.0
        for (plan in plans) {
            if (!plan.active || plan.isCard) continue
            if (plan.type != TransactionType.INCOME && plan.type != TransactionType.EXPENSE) continue
            var date = plan.nextDue
            var guard = 0
            while (!date.isAfter(to) && guard < 500) {
                guard++
                if (plan.end != null && date.isAfter(plan.end)) break
                if (!date.isBefore(from)) {
                    when (plan.type) {
                        TransactionType.INCOME -> income += plan.amount
                        TransactionType.EXPENSE -> expense += plan.amount
                        else -> {}
                    }
                }
                date = RecurrenceScheduler.advance(date, plan.frequency, plan.interval)
            }
        }
        return Expected(income, expense)
    }

    fun project(currentBalance: Double, expectedIncome: Double, expectedExpense: Double): Double =
        currentBalance + expectedIncome - expectedExpense
}
