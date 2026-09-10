package com.finzen.app.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.components.ListDivider
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.ui.theme.colorFromHex
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money

@Composable
fun AgendaScreen(
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenCard: (Long) -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenDebts: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgendaViewModel = viewModel(factory = AppViewModelProvider.Factory),
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
                text = "Agenda",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Rounded.CalendarMonth,
                title = "Nada a vencer",
                subtitle = "Faturas de cartão, lançamentos pendentes, recorrências e dívidas " +
                    "com data de vencimento aparecem aqui, em ordem.",
                modifier = Modifier.padding(top = 40.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item("summary") {
                    AgendaSummaryCard(
                        toPay = state.totalToPay,
                        toReceive = state.totalToReceive,
                        overdueCount = state.overdueCount,
                    )
                }

                items(state.groups, key = { it.label }) { group ->
                    AgendaGroupCard(
                        group = group,
                        onItemClick = { item ->
                            when (item.kind) {
                                AgendaKind.PENDING -> item.transactionId?.let(onOpenTransaction)
                                AgendaKind.CARD_INVOICE -> item.cardId?.let(onOpenCard)
                                AgendaKind.RECURRING -> onOpenRecurring()
                                AgendaKind.DEBT -> onOpenDebts()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AgendaSummaryCard(
    toPay: Double,
    toReceive: Double,
    overdueCount: Int,
) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryMetric(
                    label = "A pagar",
                    value = toPay,
                    color = FinanceTheme.colors.expense,
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "A receber",
                    value = toReceive,
                    color = FinanceTheme.colors.income,
                    modifier = Modifier.weight(1f),
                )
            }
            if (overdueCount > 0) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(FinanceTheme.colors.expense.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = if (overdueCount == 1) {
                            "1 vencimento atrasado"
                        } else {
                            "$overdueCount vencimentos atrasados"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FinanceTheme.colors.expense,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
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
private fun AgendaGroupCard(
    group: AgendaGroup,
    onItemClick: (AgendaItem) -> Unit,
) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Text(
                text = group.label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (group.overdue) FinanceTheme.colors.expense else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            group.items.forEachIndexed { index, item ->
                AgendaRow(
                    item = item,
                    showDate = group.overdue,
                    onClick = { onItemClick(item) },
                )
                if (index < group.items.lastIndex) ListDivider(72.dp)
            }
        }
    }
}

@Composable
private fun AgendaRow(
    item: AgendaItem,
    showDate: Boolean,
    onClick: () -> Unit,
) {
    val accent = item.colorHex?.let { colorFromHex(it) } ?: MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = agendaIcon(item.kind),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Money.format(item.amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (item.isInflow) FinanceTheme.colors.income else FinanceTheme.colors.expense,
                maxLines = 1,
            )
            if (showDate) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = DateUtils.dayMonthLabel(item.dueDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun agendaIcon(kind: AgendaKind): ImageVector = when (kind) {
    AgendaKind.CARD_INVOICE -> Icons.Rounded.CreditCard
    AgendaKind.PENDING -> Icons.Rounded.Schedule
    AgendaKind.RECURRING -> Icons.Rounded.Repeat
    AgendaKind.DEBT -> Icons.Rounded.Group
}
