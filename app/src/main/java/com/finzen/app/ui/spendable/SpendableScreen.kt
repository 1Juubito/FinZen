package com.finzen.app.ui.spendable

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
import androidx.compose.material.icons.rounded.WarningAmber
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
import com.finzen.app.ui.components.ListDivider
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.util.Money
import kotlin.math.abs

@Composable
fun SpendableScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpendableViewModel = viewModel(factory = AppViewModelProvider.Factory),
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
                text = "Quanto posso gastar",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HeroCard(state)
                BreakdownCard(state)
                Text(
                    text = "Considera os lançamentos recorrentes, as pendências e as faturas em aberto " +
                        "deste mês. É quanto você pode gastar e ainda fechar o mês no zero — não inclui " +
                        "novos gastos nem uma reserva.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroCard(state: SpendableUiState) {
    val accent = if (state.negative) FinanceTheme.colors.expense else FinanceTheme.colors.income
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Livre para gastar • ${state.monthLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Money.format(state.spendable),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = accent,
                maxLines = 1,
            )
            Spacer(Modifier.height(14.dp))
            if (state.negative) {
                WarningBanner(
                    text = "Suas contas previstas passam do saldo em ${Money.format(abs(state.spendable))} " +
                        "este mês.",
                )
            } else {
                val dayWord = if (state.daysLeft == 1) "dia" else "dias"
                Text(
                    text = "≈ ${Money.format(state.perDay)} por dia • ${state.daysLeft} $dayWord restantes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun BreakdownCard(state: SpendableUiState) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            BreakdownRow("Saldo de hoje", Money.format(state.balanceToday), MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            BreakdownRow("A receber este mês", "+ ${Money.format(state.incoming)}", FinanceTheme.colors.income)
            Spacer(Modifier.height(12.dp))
            BreakdownRow("A pagar este mês", "− ${Money.format(state.outgoing)}", FinanceTheme.colors.expense)
            Spacer(Modifier.height(12.dp))
            ListDivider(0.dp)
            Spacer(Modifier.height(12.dp))
            BreakdownRow(
                label = "Livre para gastar",
                value = Money.format(state.spendable),
                color = if (state.negative) FinanceTheme.colors.expense else MaterialTheme.colorScheme.onSurface,
                strong = true,
            )
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, color: Color, strong: Boolean = false) {
    val labelStyle = if (strong) {
        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val valueStyle = if (strong) {
        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = labelStyle,
            color = if (strong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = valueStyle, color = color, maxLines = 1)
    }
}

@Composable
private fun WarningBanner(text: String) {
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
