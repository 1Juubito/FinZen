package com.finzen.app.data.card

import java.time.LocalDate
import java.time.YearMonth

data class CardCycle(
    val refYear: Int,
    val refMonth: Int,
    val periodStart: LocalDate,
    val periodEndExclusive: LocalDate,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
)

object CardCycleCalculator {

    private fun clampDay(year: Int, month: Int, day: Int): LocalDate {
        val ym = YearMonth.of(year, month)
        return ym.atDay(day.coerceIn(1, ym.lengthOfMonth()))
    }

    fun cycleForClosingMonth(year: Int, month: Int, closingDay: Int, dueDay: Int): CardCycle {
        val closing = clampDay(year, month, closingDay)
        val prev = YearMonth.of(year, month).minusMonths(1)
        val previousClosing = clampDay(prev.year, prev.monthValue, closingDay)
        return CardCycle(
            refYear = year,
            refMonth = month,
            periodStart = previousClosing.plusDays(1),
            periodEndExclusive = closing.plusDays(1),
            closingDate = closing,
            dueDate = dueDateFor(closing, dueDay),
        )
    }

    fun currentCycle(today: LocalDate, closingDay: Int, dueDay: Int): CardCycle {
        val thisClosing = clampDay(today.year, today.monthValue, closingDay)
        val ref = if (!today.isAfter(thisClosing)) YearMonth.from(today) else YearMonth.from(today).plusMonths(1)
        return cycleForClosingMonth(ref.year, ref.monthValue, closingDay, dueDay)
    }

    fun cycleForOffset(today: LocalDate, closingDay: Int, dueDay: Int, offset: Int): CardCycle {
        val current = currentCycle(today, closingDay, dueDay)
        val ym = YearMonth.of(current.refYear, current.refMonth).plusMonths(offset.toLong())
        return cycleForClosingMonth(ym.year, ym.monthValue, closingDay, dueDay)
    }

    private fun dueDateFor(closing: LocalDate, dueDay: Int): LocalDate {
        val sameMonth = clampDay(closing.year, closing.monthValue, dueDay)
        if (sameMonth.isAfter(closing)) return sameMonth
        val next = YearMonth.from(closing).plusMonths(1)
        return clampDay(next.year, next.monthValue, dueDay)
    }
}
