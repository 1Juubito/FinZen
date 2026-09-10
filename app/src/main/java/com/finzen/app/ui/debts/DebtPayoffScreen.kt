package com.finzen.app.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.debt.PayoffStrategy
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.components.FilterChipPill
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.util.Money

@Composable
fun DebtPayoffScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtPayoffViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var keypadVisible by remember { mutableStateOf(false) }

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
                text = "Plano de quitação",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            !state.hasDebts -> EmptyState(
                icon = Icons.Rounded.Celebration,
                title = "Nada a quitar",
                subtitle = "Você não tem dívidas na coluna \"Eu devo\". Quando tiver, monto aqui um plano de quitação.",
                modifier = Modifier.padding(top = 40.dp),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SummaryCard(state)
                StrategyToggle(state.strategy) { viewModel.setStrategy(it) }
                BudgetField(
                    digits = state.budgetDigits,
                    onClick = { keypadVisible = true },
                    onSuggest = {
                        viewModel.setBudgetDigits((Math.round(state.totalOwed / 6.0) * 100).toString())
                        keypadVisible = false
                    },
                )
                if (keypadVisible) {
                    DoneRow { keypadVisible = false }
                    PayoffKeypad(
                        onDigit = { viewModel.appendDigit(it) },
                        onBackspace = { viewModel.backspace() },
                    )
                }
                if (state.hasReceivables) {
                    ReceivablesToggle(state.includeReceivables) { viewModel.setIncludeReceivables(it) }
                }
                state.steps.forEach { step ->
                    StepRow(step) { viewModel.settle(step.id) }
                }
                Text(
                    text = "Método bola de neve: o valor mensal abate uma dívida por vez e a sobra rola para a " +
                        "próxima. Como as dívidas não têm juros, a estratégia muda a ordem e os marcos — não o " +
                        "tempo total para zerar, que depende só do valor mensal." +
                        (if (state.includeReceivables) " Com os valores a receber ligados, assumo que você vai recebê-los nas datas previstas." else ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(state: DebtPayoffUiState) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Você deve",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Money.format(state.totalOwed),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = FinanceTheme.colors.expense,
                maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))
            val count = state.steps.size
            val info = if (state.hasBudget && state.monthsToDebtFree != null) {
                val plural = if (state.monthsToDebtFree == 1) "mês" else "meses"
                "$count ${if (count == 1) "dívida" else "dívidas"} · livre em ${state.monthsToDebtFree} $plural " +
                    "(até ${state.debtFreeLabel})"
            } else {
                "$count ${if (count == 1) "dívida" else "dívidas"} · defina um valor mensal para ver o prazo."
            }
            Text(
                text = info,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.hasReceivables) {
                Spacer(Modifier.height(8.dp))
                val net = if (state.netOwed > 0) {
                    "no líquido você deve ${Money.format(state.netOwed)}"
                } else {
                    "no líquido sobram ${Money.format(-state.netOwed)} a seu favor"
                }
                Text(
                    text = "A receber ${Money.format(state.receivableTotal)} · $net",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StrategyToggle(selected: PayoffStrategy, onSelect: (PayoffStrategy) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChipPill("Menor valor", selected == PayoffStrategy.SNOWBALL, { onSelect(PayoffStrategy.SNOWBALL) })
        FilterChipPill("Vencimento", selected == PayoffStrategy.DUE_DATE, { onSelect(PayoffStrategy.DUE_DATE) })
    }
}

@Composable
private fun BudgetField(digits: String, onClick: () -> Unit, onSuggest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column {
                Text(
                    "Pago por mês",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Money.formatCentsInput(digits),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onSuggest) { Text("Sugerir") }
    }
}

@Composable
private fun DoneRow(onDone: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = "Concluir",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { onDone() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ReceivablesToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Considerar valores a receber",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Entram como reforço no mês previsto",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StepRow(step: PayoffStepUi, onSettle: () -> Unit) {
    val subtitle = listOfNotNull(step.dueLabel, step.clearedLabel).joinToString(" • ")
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${step.position}º",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = step.person,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Money.format(step.amount) + (if (subtitle.isNotEmpty()) " • $subtitle" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onSettle) { Text("Quitar") }
        }
    }
}

@Composable
private fun PayoffKeypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "<"),
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                when (key) {
                                    "<" -> onBackspace()
                                    "000" -> repeat(3) { onDigit('0') }
                                    else -> onDigit(key.first())
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == "<") {
                            Icon(Icons.Rounded.Backspace, contentDescription = "Apagar", tint = MaterialTheme.colorScheme.onSurface)
                        } else {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
