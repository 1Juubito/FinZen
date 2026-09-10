package com.finzen.app.util

import java.time.LocalDate
import java.time.YearMonth

data class MonthRef(val year: Int, val month: Int) {

    private val yearMonth: YearMonth get() = YearMonth.of(year, month)

    fun previous(): MonthRef = yearMonth.minusMonths(1).let { MonthRef(it.year, it.monthValue) }
    fun next(): MonthRef = yearMonth.plusMonths(1).let { MonthRef(it.year, it.monthValue) }
    fun minusMonths(n: Int): MonthRef = yearMonth.minusMonths(n.toLong()).let { MonthRef(it.year, it.monthValue) }

    val label: String get() = DateUtils.monthYearLabel(year, month)
    val shortLabel: String get() = DateUtils.shortMonthLabel(year, month)
    val bounds: Pair<Long, Long> get() = DateUtils.monthBounds(year, month)

    companion object {
        fun now(): MonthRef = LocalDate.now().let { MonthRef(it.year, it.monthValue) }
    }
}
