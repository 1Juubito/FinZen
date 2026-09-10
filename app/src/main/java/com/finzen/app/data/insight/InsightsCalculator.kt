package com.finzen.app.data.insight

import com.finzen.app.util.Money
import kotlin.math.abs
import kotlin.math.roundToInt

enum class InsightTone { POSITIVE, NEGATIVE, NEUTRAL }

data class DashboardInsight(val tone: InsightTone, val text: String)

object InsightsCalculator {

    fun build(
        income: Double,
        expense: Double,
        previousExpense: Double,
        topCategoryName: String?,
        topCategoryAmount: Double,
    ): List<DashboardInsight> {
        val out = mutableListOf<DashboardInsight>()

        if (previousExpense > 0.0 && expense > 0.0) {
            val pct = ((expense - previousExpense) / previousExpense * 100).roundToInt()
            when {
                pct >= 5 -> out += DashboardInsight(
                    InsightTone.NEGATIVE,
                    "Você gastou $pct% a mais que no mês passado.",
                )
                pct <= -5 -> out += DashboardInsight(
                    InsightTone.POSITIVE,
                    "Você gastou ${abs(pct)}% a menos que no mês passado.",
                )
            }
        }

        if (topCategoryName != null && topCategoryAmount > 0.0) {
            out += DashboardInsight(
                InsightTone.NEUTRAL,
                "Maior gasto: $topCategoryName (${Money.format(topCategoryAmount)}).",
            )
        }

        if (income > 0.0 || expense > 0.0) {
            val result = income - expense
            when {
                result > 0.0 -> out += DashboardInsight(
                    InsightTone.POSITIVE,
                    "Você está economizando ${Money.format(result)} este mês.",
                )
                result < 0.0 -> out += DashboardInsight(
                    InsightTone.NEGATIVE,
                    "Seus gastos superam as receitas em ${Money.formatAbs(result)}.",
                )
            }
        }

        return out
    }
}
