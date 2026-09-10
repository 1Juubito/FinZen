package com.finzen.app.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object Money {

    private val ptBr: Locale = Locale("pt", "BR")
    private val currency: NumberFormat = NumberFormat.getCurrencyInstance(ptBr)

    fun format(value: Double): String = currency.format(value)

    fun formatAbs(value: Double): String = currency.format(abs(value))

    fun formatSigned(value: Double): String {
        val sign = if (value < 0) "-" else "+"
        return "$sign ${currency.format(abs(value))}"
    }

    fun formatCompact(value: Double): String {
        val v = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            v >= 1_000_000 -> "${sign}R$ ${trim(v / 1_000_000)} mi"
            v >= 1_000 -> "${sign}R$ ${trim(v / 1_000)} mil"
            else -> format(value)
        }
    }

    private fun trim(v: Double): String {
        val s = String.format(ptBr, "%.1f", v)
        return s.removeSuffix(",0")
    }

    fun centsToValue(digits: String): Double {
        val clean = digits.filter { it.isDigit() }.ifEmpty { "0" }
        return clean.toLong() / 100.0
    }

    fun formatCentsInput(digits: String): String = format(centsToValue(digits))
}
