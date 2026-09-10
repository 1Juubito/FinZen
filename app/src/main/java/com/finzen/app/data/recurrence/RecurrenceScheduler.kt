package com.finzen.app.data.recurrence

import com.finzen.app.data.model.RecurrenceFrequency
import java.time.LocalDate

object RecurrenceScheduler {

    fun advance(date: LocalDate, frequency: RecurrenceFrequency, interval: Int = 1): LocalDate {
        val step = interval.coerceAtLeast(1).toLong()
        return when (frequency) {
            RecurrenceFrequency.DAILY -> date.plusDays(step)
            RecurrenceFrequency.WEEKLY -> date.plusWeeks(step)
            RecurrenceFrequency.MONTHLY -> date.plusMonths(step)
            RecurrenceFrequency.YEARLY -> date.plusYears(step)
        }
    }

    fun isDue(dueDate: LocalDate, today: LocalDate): Boolean = !dueDate.isAfter(today)

    fun occurrencesPerMonth(frequency: RecurrenceFrequency, interval: Int = 1): Double {
        val step = interval.coerceAtLeast(1)
        return when (frequency) {
            RecurrenceFrequency.DAILY -> 30.436875 / step
            RecurrenceFrequency.WEEKLY -> 4.348125 / step
            RecurrenceFrequency.MONTHLY -> 1.0 / step
            RecurrenceFrequency.YEARLY -> 1.0 / (12.0 * step)
        }
    }
}
