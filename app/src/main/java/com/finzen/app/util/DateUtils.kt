package com.finzen.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {

    val ptBr: Locale = Locale("pt", "BR")
    private val zone: ZoneId = ZoneId.systemDefault()

    fun now(): Long = System.currentTimeMillis()

    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun startOfDay(millis: Long): Long =
        toLocalDate(millis).atStartOfDay(zone).toInstant().toEpochMilli()

    fun localDateToMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun toPickerUtcMillis(millis: Long): Long =
        toLocalDate(millis).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerUtcMillis(utcMillis: Long): Long =
        localDateToMillis(Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate())

    fun monthBounds(year: Int, month: Int): Pair<Long, Long> {
        val ym = YearMonth.of(year, month)
        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun monthYearLabel(year: Int, month: Int): String {
        val name = YearMonth.of(year, month).month
            .getDisplayName(TextStyle.FULL, ptBr)
            .replaceFirstChar { it.uppercase(ptBr) }
        return "$name de $year"
    }

    fun shortMonthLabel(year: Int, month: Int): String =
        YearMonth.of(year, month).month
            .getDisplayName(TextStyle.SHORT, ptBr)
            .replaceFirstChar { it.uppercase(ptBr) }
            .removeSuffix(".")

    fun dayMonthLabel(millis: Long): String {
        val d = toLocalDate(millis)
        val m = d.month.getDisplayName(TextStyle.SHORT, ptBr).removeSuffix(".")
        return "${d.dayOfMonth} de $m"
    }

    fun fullDateLabel(millis: Long): String {
        val d = toLocalDate(millis)
        val m = d.month.getDisplayName(TextStyle.SHORT, ptBr).removeSuffix(".")
        return "${d.dayOfMonth} de $m de ${d.year}"
    }

    fun dayHeaderLabel(millis: Long): String {
        val date = toLocalDate(millis)
        val today = LocalDate.now(zone)
        return when (date) {
            today -> "Hoje"
            today.minusDays(1) -> "Ontem"
            today.plusDays(1) -> "Amanhã"
            else -> {
                val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, ptBr)
                    .replaceFirstChar { it.uppercase(ptBr) }.removeSuffix(".")
                "$weekday, ${dayMonthLabel(millis)}"
            }
        }
    }
}
