package com.finzen.app.ui.burnrate

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Speed
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
import com.finzen.app.ui.components.ListDivider
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.util.Money
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BurnRateScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BurnRateViewModel = viewModel(factory = AppViewModelProvider.Factory),
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
                text = "Projeção pelo ritmo",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            !state.hasData -> EmptyState(
                icon = Icons.Rounded.Speed,
                title = "Sem gastos ainda este mês",
                subtitle = "Assim que você registrar despesas, mostramos para onde o mês caminha no seu ritmo atual.",
                modifier = Modifier.padding(top = 40.dp),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HeroCard(state)
                BreakdownCard(state)
                Text(
                    text = (if (state.early) "Começo de mês — a projeção fica mais confiável com o passar dos dias. " else "") +
                        "Estende seu gasto médio por dia até o fim do mês. Não inclui contas previstas " +
                        "que ainda não foram lançadas.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroCard(state: BurnRateUiState) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Projeção de gastos • ${state.monthLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Money.format(state.projected),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            ComparisonLine(state)
            Spacer(Modifier.height(16.dp))
            ProgressBar(state.monthProgress)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Dia ${state.daysElapsed} de ${state.daysInMonth}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ComparisonLine(state: BurnRateUiState) {
    if (!state.hasComparison) {
        Text(
            text = "Sem base do mês passado para comparar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val pct = abs(state.deltaPct).roundToInt()
    val color = if (state.aboveLastMonth) FinanceTheme.colors.expense else FinanceTheme.colors.income
    val arrow = if (state.aboveLastMonth) "↑" else "↓"
    val word = if (state.aboveLastMonth) "acima" else "abaixo"
    Text(
        text = "$arrow $pct% $word do mês passado (${Money.format(state.lastMonthTotal)})",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = color,
    )
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun BreakdownCard(state: BurnRateUiState) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            StatRow("Gasto até agora", Money.format(state.spentSoFar))
            Spacer(Modifier.height(12.dp))
            StatRow("Ritmo médio", "${Money.format(state.dailyRate)} / dia")
            Spacer(Modifier.height(12.dp))
            StatRow("Dias restantes", state.daysLeft.toString())
            Spacer(Modifier.height(12.dp))
            ListDivider(0.dp)
            Spacer(Modifier.height(12.dp))
            StatRow("Total do mês passado", Money.format(state.lastMonthTotal))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            maxLines = 1,
        )
    }
}
