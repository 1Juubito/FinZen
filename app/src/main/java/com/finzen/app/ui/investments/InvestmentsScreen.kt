package com.finzen.app.ui.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.local.entity.InvestmentEntity
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CategoryAvatar
import com.finzen.app.ui.components.DonutChart
import com.finzen.app.ui.components.DonutSlice
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.components.FilterChipPill
import com.finzen.app.ui.components.LegendItem
import com.finzen.app.ui.components.SectionHeader
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.ui.theme.colorFromHex
import com.finzen.app.util.Money
import java.util.Locale

private fun percentLabel(fraction: Double): String =
    String.format(Locale("pt", "BR"), "%+.1f%%", fraction * 100)

@Composable
fun InvestmentsScreen(
    onAddInvestment: () -> Unit,
    onEditInvestment: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvestmentsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sheetTarget by remember { mutableStateOf<InvestmentEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Investimentos",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAddInvestment) {
                Icon(Icons.Rounded.Add, contentDescription = "Novo investimento")
            }
        }

        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Rounded.ShowChart,
                title = "Nenhum investimento",
                subtitle = "Cadastre seus investimentos e acompanhe a rentabilidade da sua carteira.",
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "portfolio") {
                    PortfolioCard(state = state)
                }
                if (state.allocation.isNotEmpty()) {
                    item(key = "allocation") {
                        AllocationCard(allocation = state.allocation)
                    }
                }
                item(key = "list-header") {
                    SectionHeader(
                        title = "Meus ativos",
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    )
                }
                items(state.investments, key = { it.id }) { investment ->
                    InvestmentCard(
                        investment = investment,
                        onClick = { onEditInvestment(investment.id) },
                        onUpdate = { sheetTarget = investment },
                    )
                }
            }
        }
    }

    val target = sheetTarget
    if (target != null) {
        UpdateInvestmentSheet(
            investment = target,
            accounts = state.accounts,
            onDismiss = { sheetTarget = null },
            onContribute = { amount, sourceAccountId ->
                viewModel.contribute(target.id, amount, sourceAccountId, target.name)
                sheetTarget = null
            },
            onAddYield = { amount ->
                viewModel.addYield(target.id, amount)
                sheetTarget = null
            },
            onUpdateValue = { value ->
                viewModel.updateValue(target.id, value)
                sheetTarget = null
            },
        )
    }
}

@Composable
private fun PortfolioCard(
    state: InvestmentsUiState,
    modifier: Modifier = Modifier,
) {
    val profitPositive = state.totalProfit >= 0.0
    val profitColor = if (profitPositive) FinanceTheme.colors.income else FinanceTheme.colors.expense
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = "Patrimônio total",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = Money.format(state.totalCurrent),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            HeroStat(
                label = "Aplicado",
                value = Money.format(state.totalInvested),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            HeroStat(
                label = "Rendimento",
                value = "${Money.formatSigned(state.totalProfit)}  (${percentLabel(state.profitability)})",
                valueColor = profitColor,
                icon = if (profitPositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                modifier = Modifier.weight(1.2f),
            )
        }
    }
}

@Composable
private fun HeroStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AllocationCard(
    allocation: List<AllocationSlice>,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Distribuição da carteira",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                DonutChart(
                    slices = allocation.map { DonutSlice(it.total.toFloat(), colorFromHex(it.colorHex)) },
                    modifier = Modifier.size(186.dp),
                    strokeWidth = 26.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ativos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${allocation.size}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                allocation.take(6).forEach { slice ->
                    LegendItem(
                        color = colorFromHex(slice.colorHex),
                        label = slice.label,
                        value = Money.formatCompact(slice.total),
                        percent = "${(slice.fraction * 100).toInt()}%",
                    )
                }
            }
        }
    }
}

@Composable
private fun InvestmentCard(
    investment: InvestmentEntity,
    onClick: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profit = investment.currentValue - investment.investedAmount
    val profitability = if (investment.investedAmount > 0.0) profit / investment.investedAmount else 0.0
    val profitPositive = profit >= 0.0
    val profitColor = if (profitPositive) FinanceTheme.colors.income else FinanceTheme.colors.expense
    val accent = colorFromHex(investment.colorHex)

    NeoCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryAvatar(colorHex = investment.colorHex, iconKey = investment.type.iconKey, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = investment.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (investment.institution.isBlank()) {
                        investment.type.label
                    } else {
                        "${investment.type.label} • ${investment.institution}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(investment.currentValue),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (profitPositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = profitColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = percentLabel(profitability),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = profitColor,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = onUpdate,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Aporte ou atualização",
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private enum class UpdateMode { CONTRIBUTE, YIELD, SET_VALUE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateInvestmentSheet(
    investment: InvestmentEntity,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onContribute: (amount: Double, sourceAccountId: Long?) -> Unit,
    onAddYield: (amount: Double) -> Unit,
    onUpdateValue: (value: Double) -> Unit,
) {
    var digits by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(UpdateMode.CONTRIBUTE) }
    var sourceAccountId by remember {
        mutableStateOf(accounts.firstOrNull { it.includeInTotal }?.id ?: accounts.firstOrNull()?.id)
    }
    val accent = colorFromHex(investment.colorHex)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val amount = Money.centsToValue(digits)
    val canConfirm = amount > 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Text(
                text = investment.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Aplicado ${Money.format(investment.investedAmount)}  •  Atual ${Money.format(investment.currentValue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SheetToggle(
                    label = "Aporte",
                    selected = mode == UpdateMode.CONTRIBUTE,
                    selectedColor = FinanceTheme.colors.income,
                    modifier = Modifier.weight(1f),
                    onClick = { mode = UpdateMode.CONTRIBUTE },
                )
                SheetToggle(
                    label = "Rendimento",
                    selected = mode == UpdateMode.YIELD,
                    selectedColor = FinanceTheme.colors.income,
                    modifier = Modifier.weight(1f),
                    onClick = { mode = UpdateMode.YIELD },
                )
                SheetToggle(
                    label = "Saldo",
                    selected = mode == UpdateMode.SET_VALUE,
                    selectedColor = accent,
                    modifier = Modifier.weight(1f),
                    onClick = { mode = UpdateMode.SET_VALUE },
                )
            }

            if (mode == UpdateMode.CONTRIBUTE && accounts.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "De qual conta saiu? (opcional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    accounts.forEach { acc ->
                        FilterChipPill(
                            label = acc.name,
                            selected = sourceAccountId == acc.id,
                            onClick = { sourceAccountId = acc.id },
                            selectedColor = FinanceTheme.colors.income,
                        )
                    }
                    FilterChipPill(
                        label = "De fora",
                        selected = sourceAccountId == null,
                        onClick = { sourceAccountId = null },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = when (mode) {
                        UpdateMode.CONTRIBUTE -> "Valor do aporte"
                        UpdateMode.YIELD -> "Valor do rendimento"
                        UpdateMode.SET_VALUE -> "Novo saldo atual"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Money.formatCentsInput(digits),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (mode == UpdateMode.SET_VALUE) accent else FinanceTheme.colors.income,
                )
            }

            SheetKeypad(
                onDigit = { d -> digits = (digits + d).trimStart('0').take(12) },
                onBackspace = { digits = digits.dropLast(1) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Button(
                onClick = {
                    when (mode) {
                        UpdateMode.CONTRIBUTE -> onContribute(amount, sourceAccountId)
                        UpdateMode.YIELD -> onAddYield(amount)
                        UpdateMode.SET_VALUE -> onUpdateValue(amount)
                    }
                },
                enabled = canConfirm,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(54.dp),
            ) {
                Text(
                    text = "Confirmar",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SheetToggle(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SheetKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "<"),
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
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
                            Icon(
                                Icons.Rounded.Backspace,
                                contentDescription = "Apagar",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
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
