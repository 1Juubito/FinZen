package com.finzen.app.ui.forecast

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.components.ListDivider
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.util.Money
import kotlin.math.abs

@Composable
fun CashFlowScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CashFlowViewModel = viewModel(factory = AppViewModelProvider.Factory),
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
                text = "Previsão",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Rounded.Timeline,
                title = "Montando sua previsão",
                subtitle = "A projeção usa seu saldo de hoje, os lançamentos recorrentes, " +
                    "as pendências e as faturas em aberto.",
                modifier = Modifier.padding(top = 40.dp),
            )
        } else {
            val maxAbs = state.months.maxOf { abs(it.endBalance) }.coerceAtLeast(1.0)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item("summary") { ForecastSummaryCard(state) }
                item("months") { MonthsCard(state.months, maxAbs) }
                item("note") {
                    Text(
                        text = "Estimativa com base nos recorrentes, pendências e faturas atuais. " +
                            "Não inclui gastos novos e imprevistos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastSummaryCard(state: CashFlowUiState) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Metric(
                    label = "Saldo hoje",
                    value = state.startBalance,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Metric(
                    label = "Em ${state.months.size} meses",
                    value = state.endBalance,
                    color = if (state.endBalance < 0.0) FinanceTheme.colors.expense else FinanceTheme.colors.income,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))
            if (state.hasShortfall) {
                ShortfallBanner(
                    text = "Seu saldo fica negativo: menor ponto ${Money.format(state.lowestBalance)} " +
                        "em ${state.lowestLabel}.",
                )
            } else {
                Text(
                    text = "Menor saldo no período: ${Money.format(state.lowestBalance)} em ${state.lowestLabel}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ShortfallBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FinanceTheme.colors.expense.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = FinanceTheme.colors.expense,
            modifier = Modifier.width(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = FinanceTheme.colors.expense,
        )
    }
}

@Composable
private fun Metric(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = Money.format(value),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthsCard(months: List<CashFlowMonthUi>, maxAbs: Double) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            months.forEachIndexed { index, month ->
                MonthRow(month, maxAbs)
                if (index < months.lastIndex) ListDivider(16.dp)
            }
        }
    }
}

@Composable
private fun MonthRow(month: CashFlowMonthUi, maxAbs: Double) {
    val negative = month.endBalance < 0.0
    val barColor = if (negative) FinanceTheme.colors.expense else FinanceTheme.colors.income
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = month.label + if (month.isCurrent) " • atual" else "",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (month.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = if (month.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Money.format(month.endBalance),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (negative) FinanceTheme.colors.expense else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((abs(month.endBalance) / maxAbs).toFloat().coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FlowTag("Entra", month.income, FinanceTheme.colors.income)
            FlowTag("Sai", month.expense, FinanceTheme.colors.expense)
        }
    }
}

@Composable
private fun FlowTag(label: String, value: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Money.formatCompact(value),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}
