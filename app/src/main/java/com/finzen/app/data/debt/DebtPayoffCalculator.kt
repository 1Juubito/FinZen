package com.finzen.app.data.debt

enum class PayoffStrategy { SNOWBALL, DUE_DATE }

data class PayoffDebt(
    val id: Long,
    val person: String,
    val amount: Double,
    val dueDate: Long?,
)

data class PayoffInflow(val amount: Double, val monthOffset: Int)

data class PayoffStep(
    val debt: PayoffDebt,
    val position: Int,

    val clearedMonthOffset: Int?,
)

data class PayoffPlan(
    val steps: List<PayoffStep>,
    val totalOwed: Double,

    val monthsToDebtFree: Int?,
)

object DebtPayoffCalculator {

    private const val EPSILON = 1e-6

    fun plan(
        debts: List<PayoffDebt>,
        strategy: PayoffStrategy,
        monthlyBudget: Double,
        inflows: List<PayoffInflow> = emptyList(),
    ): PayoffPlan {
        val ordered = when (strategy) {
            PayoffStrategy.SNOWBALL -> debts.sortedBy { it.amount }
            PayoffStrategy.DUE_DATE ->
                debts.sortedWith(compareBy<PayoffDebt> { it.dueDate ?: Long.MAX_VALUE }.thenBy { it.amount })
        }
        val total = debts.sumOf { it.amount }

        if (debts.isEmpty() || monthlyBudget <= 0.0) {
            return PayoffPlan(ordered.mapIndexed { i, d -> PayoffStep(d, i + 1, null) }, total, null)
        }

        val inflowByMonth = inflows.groupBy { it.monthOffset }.mapValues { (_, v) -> v.sumOf { it.amount } }
        var month = 0
        var monthLeft = monthlyBudget + (inflowByMonth[0] ?: 0.0)
        val steps = ArrayList<PayoffStep>(ordered.size)
        for (debt in ordered) {
            var remaining = debt.amount
            while (remaining > EPSILON) {
                if (monthLeft <= EPSILON) {
                    month++
                    monthLeft = monthlyBudget + (inflowByMonth[month] ?: 0.0)
                }
                val pay = minOf(remaining, monthLeft)
                remaining -= pay
                monthLeft -= pay
            }
            steps.add(PayoffStep(debt, steps.size + 1, month))
        }
        return PayoffPlan(steps, total, month + 1)
    }
}
