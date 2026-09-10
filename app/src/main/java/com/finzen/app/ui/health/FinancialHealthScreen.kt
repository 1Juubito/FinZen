package com.finzen.app.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material3.CircularProgressIndicator
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
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money
import kotlin.math.roundToInt

private val Amber = Color(0xFFF59E0B)

@Composable
fun FinancialHealthScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinancialHealthViewModel = viewModel(factory = AppViewModelProvider.Factory),
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
                text = "Saúde financeira",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            !state.hasData -> EmptyState(
                icon = Icons.Rounded.MonitorHeart,
                title = "Ainda sem histórico",
                subtitle = "Registre receitas e despesas por alguns meses e mostramos seus indicadores de saúde financeira.",
                modifier = Modifier.padding(top = 40.dp),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OverallCard(state.overall)
                SavingsCard(state)
                ReserveCard(state)
                CommitmentCard(state)
                Text(
                    text = "Médias dos últimos ${FinancialHealthViewModel.MONTHS} meses. Reserva = saldo de hoje " +
                        "÷ despesa média mensal. Comprometimento = contas fixas (recorrentes) sobre a renda média.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun OverallCard(level: HealthLevel) {
    val color = levelColor(level)
    val title = when (level) {
        HealthLevel.GOOD -> "Saudável"
        HealthLevel.OK -> "Atenção"
        HealthLevel.BAD -> "Crítico"
    }
    val subtitle = when (level) {
        HealthLevel.GOOD -> "Suas finanças estão equilibradas. Continue assim!"
        HealthLevel.OK -> "No geral ok, mas dá para melhorar em alguns pontos."
        HealthLevel.BAD -> "Alguns indicadores pedem atenção com prioridade."
    }
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Saúde geral",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = color,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SavingsCard(state: FinancialHealthUiState) {
    val value = if (state.hasIncome) "${(state.savingsRate * 100).roundToInt()}%" else "—"
    val explanation = if (state.hasIncome) {
        "Você guarda ${(state.savingsRate * 100).roundToInt()}% do que ganha (≈ ${Money.format(state.monthlySurplus)}/mês)."
    } else {
        "Sem receitas registradas no período para calcular."
    }
    MetricCard("Taxa de poupança", value, explanation, state.savingsLevel.takeIf { state.hasIncome })
}

@Composable
private fun ReserveCard(state: FinancialHealthUiState) {
    val capped = state.reserveMonths > 12.0
    val value = when {
        !state.reserveReliable -> "—"
        capped -> "12+ meses"
        else -> "${oneDecimal(state.reserveMonths)} meses"
    }
    val explanation = when {
        !state.reserveReliable -> "Poucos gastos registrados no período para uma reserva confiável."
        capped -> "Seu saldo cobre mais de um ano de despesa."
        else -> "Seu saldo de hoje cobre ${oneDecimal(state.reserveMonths)} meses de despesa."
    }
    MetricCard("Reserva de emergência", value, explanation, state.reserveLevel.takeIf { state.reserveReliable })
}

@Composable
private fun CommitmentCard(state: FinancialHealthUiState) {
    val value = if (state.hasIncome) "${(state.commitment * 100).roundToInt()}%" else "—"
    val explanation = if (state.hasIncome) {
        "${(state.commitment * 100).roundToInt()}% da sua renda já está comprometida com contas fixas " +
            "(${Money.format(state.fixedMonthly)}/mês)."
    } else {
        "Sem receitas registradas no período para calcular."
    }
    MetricCard("Comprometimento com contas fixas", value, explanation, state.commitmentLevel.takeIf { state.hasIncome })
}

@Composable
private fun MetricCard(title: String, value: String, explanation: String, level: HealthLevel?) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (level != null) LevelChip(level)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (level != null) levelColor(level) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LevelChip(level: HealthLevel) {
    val color = levelColor(level)
    val label = when (level) {
        HealthLevel.GOOD -> "Ótimo"
        HealthLevel.OK -> "Atenção"
        HealthLevel.BAD -> "Crítico"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

@Composable
private fun levelColor(level: HealthLevel): Color = when (level) {
    HealthLevel.GOOD -> FinanceTheme.colors.income
    HealthLevel.OK -> Amber
    HealthLevel.BAD -> FinanceTheme.colors.expense
}

private fun oneDecimal(value: Double): String = String.format(DateUtils.ptBr, "%.1f", value)
