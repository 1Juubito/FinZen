package com.finzen.app.ui.recurring

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.local.entity.RecurringTransactionEntity
import com.finzen.app.data.model.TransactionType
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CategoryAvatar
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val recurrences by viewModel.items.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

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
                text = "Lançamentos recorrentes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        if (recurrences.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(recurrences, key = { it.entity.id }) { item ->
                    RecurringCard(item = item, onClick = { selected = item.entity })
                }
            }
        }
    }

    val current = selected
    if (current != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { selected = null }, sheetState = sheetState) {
            Text(
                text = current.description,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            ActionRow(Icons.Rounded.PlayCircle, "Lançar agora", MaterialTheme.colorScheme.primary) {
                viewModel.postNow(current)
                selected = null
            }
            ActionRow(Icons.Rounded.SkipNext, "Pular próxima", MaterialTheme.colorScheme.onSurface) {
                viewModel.skip(current)
                selected = null
            }
            if (current.active) {
                ActionRow(Icons.Rounded.PauseCircle, "Pausar", MaterialTheme.colorScheme.onSurface) {
                    viewModel.setActive(current, false)
                    selected = null
                }
            } else {
                ActionRow(Icons.Rounded.PlayCircle, "Retomar", MaterialTheme.colorScheme.onSurface) {
                    viewModel.setActive(current, true)
                    selected = null
                }
            }
            ActionRow(Icons.Rounded.DeleteOutline, "Excluir", FinanceTheme.colors.expense) {
                viewModel.delete(current)
                selected = null
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecurringCard(item: RecurringItemUi, onClick: () -> Unit) {
    val e = item.entity
    val amountColor = when (e.type) {
        TransactionType.INCOME -> FinanceTheme.colors.income
        else -> FinanceTheme.colors.expense
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryAvatar(colorHex = item.categoryColor, iconKey = item.categoryIcon, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = e.description,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${e.frequency.label} • ${statusLabel(e)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!e.active) MaterialTheme.colorScheme.onSurfaceVariant else statusColor(e),
                )
                item.paymentName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = Money.format(e.amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
            )
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Repeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Nenhum lançamento recorrente",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Marque \"Repetir\" ao adicionar uma despesa ou receita para criar uma recorrência. " +
                "Quando vencer, o app te lembra de confirmar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun statusLabel(e: RecurringTransactionEntity): String {
    if (!e.active) return "Pausada"
    val due = DateUtils.toLocalDate(e.nextDueDate)
    val today = LocalDate.now()
    return when {
        due.isBefore(today) -> "Atrasada desde ${DateUtils.dayMonthLabel(e.nextDueDate)}"
        due.isEqual(today) -> "Vence hoje"
        else -> "Próxima ${DateUtils.dayMonthLabel(e.nextDueDate)}"
    }
}

@Composable
private fun statusColor(e: RecurringTransactionEntity): androidx.compose.ui.graphics.Color {
    val due = DateUtils.toLocalDate(e.nextDueDate)
    val today = LocalDate.now()
    return if (!due.isAfter(today)) FinanceTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant
}
