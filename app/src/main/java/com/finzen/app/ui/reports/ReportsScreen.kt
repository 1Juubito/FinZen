package com.finzen.app.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.model.TransactionType
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.ColorDot
import com.finzen.app.ui.components.DonutChart
import com.finzen.app.ui.components.DonutSlice
import com.finzen.app.ui.components.FilterChipPill
import com.finzen.app.ui.components.IncomeExpenseBars
import com.finzen.app.ui.components.LegendItem
import com.finzen.app.ui.components.LineChart
import com.finzen.app.ui.components.LinePoint
import com.finzen.app.ui.components.MonthBarData
import com.finzen.app.ui.components.MonthNavigator
import com.finzen.app.ui.components.SavingsGauge
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.ui.theme.colorFromHex
import com.finzen.app.util.Money
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                text = "Relatórios",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MonthNavigator(
                    label = state.monthRef.label,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                TypeToggle(
                    selected = state.type,
                    onSelect = viewModel::setType,
                )
            }

            item {
                BreakdownCard(
                    type = state.type,
                    slices = state.slices,
                    total = state.total,
                )
            }

            item {
                ComparisonCard(
                    type = state.type,
                    comparisons = state.comparisons,
                )
            }

            item {
                BalanceCard(
                    income = state.monthIncome,
                    expense = state.monthExpense,
                    balance = state.monthBalance,
                )
            }

            item {
                SavingsCard(
                    income = state.monthIncome,
                    expense = state.monthExpense,
                )
            }

            item {
                FrequencyCard(
                    period = state.frequencyPeriod,
                    points = state.frequencyPoints,
                    total = state.frequencyTotal,
                    onSelectPeriod = viewModel::setFrequencyPeriod,
                )
            }

            item {
                SixMonthCard(months = state.months)
            }
        }
    }
}

@Composable
private fun TypeToggle(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilterChipPill(
            label = "Despesas",
            selected = selected == TransactionType.EXPENSE,
            onClick = { onSelect(TransactionType.EXPENSE) },
            selectedColor = FinanceTheme.colors.expense,
        )
        FilterChipPill(
            label = "Receitas",
            selected = selected == TransactionType.INCOME,
            onClick = { onSelect(TransactionType.INCOME) },
            selectedColor = FinanceTheme.colors.income,
        )
    }
}

@Composable
private fun BreakdownCard(
    type: TransactionType,
    slices: List<CategorySlice>,
    total: Double,
    modifier: Modifier = Modifier,
) {
    val accent = if (type == TransactionType.INCOME) {
        FinanceTheme.colors.income
    } else {
        FinanceTheme.colors.expense
    }
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Por categoria",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            if (slices.isEmpty()) {
                Text(
                    text = if (type == TransactionType.INCOME) {
                        "Sem receitas neste mês."
                    } else {
                        "Sem despesas neste mês."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    DonutChart(
                        slices = slices.map { DonutSlice(it.total.toFloat(), colorFromHex(it.colorHex)) },
                        modifier = Modifier.size(186.dp),
                        strokeWidth = 26.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = Money.formatCompact(total),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = accent,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    slices.forEach { slice ->
                        LegendItem(
                            color = colorFromHex(slice.colorHex),
                            label = slice.name,
                            value = Money.formatCompact(slice.total),
                            percent = "${(slice.fraction * 100).toInt()}%",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonCard(
    type: TransactionType,
    comparisons: List<CategoryComparison>,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Comparativo com o mês anterior",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            if (comparisons.isEmpty()) {
                Text(
                    text = "Sem dados para comparar com o mês anterior.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    comparisons.forEach { ComparisonRow(it, type) }
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    item: CategoryComparison,
    type: TransactionType,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        ColorDot(color = colorFromHex(item.colorHex), size = 12.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "antes ${Money.formatCompact(item.previous)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Money.formatCompact(item.current),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            DeltaBadge(item, type)
        }
    }
}

@Composable
private fun DeltaBadge(item: CategoryComparison, type: TransactionType) {

    val favorable: Boolean? = when {
        item.delta == 0.0 -> null
        type == TransactionType.EXPENSE -> item.delta < 0
        else -> item.delta > 0
    }
    val color = when (favorable) {
        true -> FinanceTheme.colors.income
        false -> FinanceTheme.colors.expense
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    when {
        item.isNew -> Text(
            text = "novo",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
        item.pctChange == null || item.delta == 0.0 -> Text(
            text = "—",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (item.delta > 0) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = "${abs(item.pctChange!! * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = color,
            )
        }
    }
}

@Composable
private fun BalanceCard(
    income: Double,
    expense: Double,
    balance: Double,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Balanço mensal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                val maxValue = maxOf(income, expense, 1.0)
                Row(
                    modifier = Modifier.height(120.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BalanceBar(120.dp * (income / maxValue).toFloat(), FinanceTheme.colors.income)
                    BalanceBar(120.dp * (expense / maxValue).toFloat(), FinanceTheme.colors.expense)
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    StatLine("Receitas", Money.format(income), FinanceTheme.colors.income)
                    Spacer(Modifier.height(10.dp))
                    StatLine("Despesas", Money.format(expense), FinanceTheme.colors.expense)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Spacer(Modifier.height(12.dp))
                    StatLine(
                        "Balanço",
                        Money.format(balance),
                        if (balance < 0) FinanceTheme.colors.expense else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceBar(height: Dp, color: Color) {
    Box(
        Modifier
            .width(26.dp)
            .height(height.coerceAtLeast(6.dp))
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .background(color),
    )
}

@Composable
private fun StatLine(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
        )
    }
}

@Composable
private fun SavingsCard(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier,
) {
    val balance = income - expense
    val saved = balance >= 0.0
    val color = if (saved) FinanceTheme.colors.income else FinanceTheme.colors.expense
    val progress = when {
        income <= 0.0 -> if (expense > 0.0) 1f else 0f
        saved -> (balance / income).toFloat().coerceIn(0f, 1f)
        else -> (-balance / income).toFloat().coerceIn(0f, 1f)
    }
    val displayPct = when {
        income <= 0.0 -> 0
        saved -> (balance / income * 100.0).roundToInt()
        else -> (-balance / income * 100.0).roundToInt()
    }
    val centerText = if (income <= 0.0 && expense > 0.0) "—" else "$displayPct%"
    val headline = when {
        income <= 0.0 && expense <= 0.0 -> "Sem movimentações neste mês."
        income <= 0.0 -> "Você gastou ${Money.format(expense)} sem receita registrada neste mês."
        saved -> "Você guardou ${Money.format(balance)} este mês — $displayPct% da sua renda."
        else -> "Este mês você gastou ${Money.format(abs(balance))} a mais do que ganhou ($displayPct% a mais)."
    }
    val tip = if (saved) {
        "Mandou bem! Continue assim e considere reforçar suas metas."
    } else {
        "Calma, sem pânico. Reveja as próximas despesas e seu planejamento."
    }

    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Economia mensal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SavingsGauge(progress = progress, color = color, modifier = Modifier.size(120.dp)) {
                    Text(
                        text = centerText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = color,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Color(0xFFF7B731),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FrequencyCard(
    period: FrequencyPeriod,
    points: List<LinePoint>,
    total: Double,
    onSelectPeriod: (FrequencyPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Frequência de gastos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FrequencyPeriod.values().forEach { p ->
                    FilterChipPill(p.label, period == p, { onSelectPeriod(p) })
                }
            }
            Spacer(Modifier.height(16.dp))
            if (total <= 0.0) {
                Text(
                    text = "Sem gastos no período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LineChart(
                    points = points,
                    lineColor = FinanceTheme.colors.expense,
                    valueLabel = { Money.formatCompact(it.toDouble()) },
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = Money.format(total),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SixMonthCard(
    months: List<MonthBarData>,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Receitas x Despesas (6 meses)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            ChartLegendRow(
                incomeColor = FinanceTheme.colors.income,
                expenseColor = FinanceTheme.colors.expense,
            )
            Spacer(Modifier.height(20.dp))
            if (months.isEmpty()) {
                Text(
                    text = "Sem dados para o período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                IncomeExpenseBars(
                    data = months,
                    incomeColor = FinanceTheme.colors.income,
                    expenseColor = FinanceTheme.colors.expense,
                )
            }
        }
    }
}

@Composable
private fun ChartLegendRow(
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegendTag(color = incomeColor, label = "Receitas")
        LegendTag(color = expenseColor, label = "Despesas")
    }
}

@Composable
private fun LegendTag(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(color = color, size = 10.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
