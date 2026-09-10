package com.finzen.app.ui.dashboard

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.insight.DashboardInsight
import com.finzen.app.data.insight.InsightTone
import com.finzen.app.data.model.AccountWithBalance
import com.finzen.app.data.model.TransactionType
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CategoryAvatar
import com.finzen.app.ui.components.DonutChart
import com.finzen.app.ui.components.DonutSlice
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.components.LegendItem
import com.finzen.app.ui.components.ListDivider
import com.finzen.app.ui.components.MonthNavigator
import com.finzen.app.ui.components.SectionHeader
import com.finzen.app.ui.components.TransactionRow
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.ui.theme.colorFromHex
import com.finzen.app.util.Money

@Composable
fun DashboardScreen(
    onAddTransaction: (TransactionType) -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenDebts: () -> Unit,
    onOpenCards: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { DashboardTopBar(name = state.userName) }

        item {
            CleanBalanceHeader(
                balance = state.totalBalance,
                income = state.income,
                expense = state.expense,
                visible = state.balanceVisible,
                monthLabel = state.monthRef.label,
                onToggleVisibility = viewModel::toggleBalanceVisibility,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item {
            QuickActions(
                onAddTransaction = onAddTransaction,
                onOpenReports = onOpenReports,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        state.projection?.takeIf { state.balanceVisible }?.let { projection ->
            item {
                ProjectionCard(
                    projection = projection,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (state.insights.isNotEmpty() && state.balanceVisible) {
            item {
                InsightsCard(
                    insights = state.insights,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (state.hasOpenInvoices && state.balanceVisible) {
            item {
                OpenInvoicesDashboardCard(
                    invoices = state.openInvoices,
                    total = state.openInvoicesTotal,
                    onClick = onOpenCards,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (state.hasDebts && state.balanceVisible) {
            item {
                DebtsDashboardCard(
                    theyOwe = state.debtsTheyOwe,
                    iOwe = state.debtsIOwe,
                    onClick = onOpenDebts,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item {
            ExpenseDonutCard(
                slices = state.expenseSlices,
                total = state.totalExpenseForChart,
                visible = state.balanceVisible,
                onSeeReports = onOpenReports,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.accounts.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(
                        title = "Minhas contas",
                        actionLabel = "Ver todas",
                        onAction = onOpenAccounts,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    AccountsStrip(
                        accounts = state.accounts,
                        visible = state.balanceVisible,
                        onOpenAccounts = onOpenAccounts,
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Transações recentes",
                actionLabel = "Ver tudo",
                onAction = onOpenTransactions,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.recent.isEmpty()) {
            item {
                NeoCard(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                ) {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "Nenhuma transação ainda",
                        subtitle = "Toque no botão + para registrar sua primeira receita ou despesa.",
                    )
                }
            }
        } else {
            item {
                NeoCard(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                ) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        state.recent.forEachIndexed { index, item ->
                            TransactionRow(
                                item = item,
                                onClick = { onOpenTransaction(item.transaction.id) },
                                showDate = false,
                            )
                            if (index < state.recent.lastIndex) ListDivider(72.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTopBar(name: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (name.isBlank()) "Olá, bem-vindo 👋" else "Olá, bem-vindo, $name 👋",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Visão geral",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CleanBalanceHeader(
    balance: Double,
    income: Double,
    expense: Double,
    visible: Boolean,
    monthLabel: String,
    onToggleVisibility: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Saldo total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                contentDescription = "Mostrar ou ocultar saldo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onToggleVisibility() }
                    .padding(6.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (visible) Money.format(balance) else "R$ ••••••",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(18.dp))
        MonthNavigator(
            label = monthLabel,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        val result = income - expense
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CleanMetric("Receitas", income, FinanceTheme.colors.income, visible)
            CleanMetric("Despesas", expense, FinanceTheme.colors.expense, visible)
            CleanMetric(
                "Resultado",
                result,
                if (result < 0) FinanceTheme.colors.expense else FinanceTheme.colors.income,
                visible,
            )
        }
    }
}

@Composable
private fun CleanMetric(label: String, value: Double, color: Color, visible: Boolean) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (visible) Money.format(value) else "••••",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProjectionCard(
    projection: ProjectionUi,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Disponível",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Saldo livre se quitar as pendências do mês",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = Money.format(projection.projectedBalance),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (projection.projectedBalance < 0) {
                    FinanceTheme.colors.expense
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                CleanMetric("A receber", projection.expectedIncome, FinanceTheme.colors.income, visible = true)
                CleanMetric("A pagar", projection.expectedExpense, FinanceTheme.colors.expense, visible = true)
            }
        }
    }
}

@Composable
private fun InsightsCard(
    insights: List<DashboardInsight>,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            insights.forEach { insight -> InsightRow(insight) }
        }
    }
}

@Composable
private fun InsightRow(insight: DashboardInsight) {
    val color = when (insight.tone) {
        InsightTone.POSITIVE -> FinanceTheme.colors.income
        InsightTone.NEGATIVE -> FinanceTheme.colors.expense
        InsightTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (insight.tone) {
        InsightTone.POSITIVE -> Icons.Rounded.TrendingUp
        InsightTone.NEGATIVE -> Icons.Rounded.TrendingDown
        InsightTone.NEUTRAL -> Icons.Rounded.Lightbulb
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = insight.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickActions(
    onAddTransaction: (TransactionType) -> Unit,
    onOpenReports: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickAction(
            label = "Receita",
            icon = Icons.Rounded.ArrowUpward,
            tint = FinanceTheme.colors.income,
            onClick = { onAddTransaction(TransactionType.INCOME) },
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            label = "Despesa",
            icon = Icons.Rounded.ArrowDownward,
            tint = FinanceTheme.colors.expense,
            onClick = { onAddTransaction(TransactionType.EXPENSE) },
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            label = "Transferir",
            icon = Icons.Rounded.SwapHoriz,
            tint = FinanceTheme.colors.transfer,
            onClick = { onAddTransaction(TransactionType.TRANSFER) },
            modifier = Modifier.weight(1f),
        )
        QuickAction(
            label = "Relatórios",
            icon = Icons.Rounded.BarChart,
            tint = MaterialTheme.colorScheme.tertiary,
            onClick = onOpenReports,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OpenInvoicesDashboardCard(
    invoices: List<DashboardCardInvoice>,
    total: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Faturas em aberto",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (invoices.size == 1) {
                        Text(
                            text = invoices.first().name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = Money.format(total),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = FinanceTheme.colors.expense,
                        maxLines = 1,
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (invoices.size > 1) {
                Spacer(Modifier.height(12.dp))
                invoices.forEach { invoice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colorFromHex(invoice.colorHex)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = invoice.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = Money.format(invoice.amount),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtsDashboardCard(
    theyOwe: Double,
    iOwe: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Dívidas",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (theyOwe > 0.0) DebtMini("Me devem", theyOwe, FinanceTheme.colors.income)
                    if (iOwe > 0.0) DebtMini("Eu devo", iOwe, FinanceTheme.colors.expense)
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DebtMini(label: String, value: Double, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = Money.format(value),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun ExpenseDonutCard(
    slices: List<CategorySliceUi>,
    total: Double,
    visible: Boolean,
    onSeeReports: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            SectionHeader(
                title = "Despesas por categoria",
                actionLabel = "Detalhes",
                onAction = onSeeReports,
            )
            Spacer(Modifier.height(16.dp))
            if (slices.isEmpty()) {
                Text(
                    text = "Sem despesas neste mês.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        DonutChart(
                            slices = slices.map { DonutSlice(it.total.toFloat(), colorFromHex(it.colorHex)) },
                            modifier = Modifier.size(168.dp),
                            strokeWidth = 24.dp,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (visible) Money.formatCompact(total) else "••••",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    slices.take(5).forEach { slice ->
                        LegendItem(
                            color = colorFromHex(slice.colorHex),
                            label = slice.name,
                            value = if (visible) Money.formatCompact(slice.total) else "••••",
                            percent = "${(slice.fraction * 100).toInt()}%",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountsStrip(
    accounts: List<AccountWithBalance>,
    visible: Boolean,
    onOpenAccounts: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(accounts) { item ->
            NeoCard(
                modifier = Modifier
                    .width(170.dp)
                    .clickable { onOpenAccounts() },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(
                            colorHex = item.account.colorHex,
                            iconKey = item.account.iconKey,
                            size = 38.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = item.account.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Saldo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (visible) Money.format(item.balance) else "••••",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (item.balance < 0) FinanceTheme.colors.expense else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
