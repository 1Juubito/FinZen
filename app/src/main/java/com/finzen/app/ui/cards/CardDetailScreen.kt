package com.finzen.app.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CategoryAvatar
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.components.ListDivider
import com.finzen.app.ui.components.MonthNavigator
import com.finzen.app.ui.components.TransactionRow
import com.finzen.app.ui.theme.LocalIsDarkTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.ui.theme.colorFromHex
import com.finzen.app.util.Money
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    onBack: () -> Unit,
    onEditCard: (Long) -> Unit,
    onAddExpense: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cardId = viewModel.cardId
    var showPaySheet by remember { mutableStateOf(false) }

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
                text = state.card?.name ?: "Cartão",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onEditCard(cardId) }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Editar cartão")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                InvoiceHeaderCard(
                    colorHex = state.card?.colorHex ?: CardEditUiState.DEFAULT_CARD_COLOR,
                    invoiceTotal = state.invoiceTotal,
                    statusLabel = statusLabel(state.status),
                    periodInfo = periodInfo(state),
                )
            }

            item {
                MonthNavigator(
                    label = state.cycleLabel,
                    onPrevious = viewModel::previousCycle,
                    onNext = viewModel::nextCycle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Button(
                    onClick = { onAddExpense(cardId) },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Adicionar despesa",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            if (state.status != InvoiceStatus.EMPTY) {
                item {
                    OutlinedButton(
                        onClick = {
                            if (state.status == InvoiceStatus.PAID) viewModel.reopenInvoice()
                            else showPaySheet = true
                        },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text(
                            text = if (state.status == InvoiceStatus.PAID) "Reabrir fatura" else "Marcar fatura como paga",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }

            if (state.items.isEmpty()) {
                item {
                    NeoCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        EmptyState(
                            icon = Icons.Rounded.CreditCard,
                            title = "Sem lançamentos",
                            subtitle = "Nenhuma despesa nesta fatura (${state.cycleLabel}).",
                        )
                    }
                }
            } else {
                item {
                    NeoCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            state.items.forEachIndexed { index, item ->
                                TransactionRow(
                                    item = item,
                                    onClick = { onOpenTransaction(item.transaction.id) },
                                    showDate = true,
                                )
                                if (index < state.items.lastIndex) ListDivider(72.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showPaySheet = false }, sheetState = sheetState) {
            Text(
                text = "Pagar fatura com",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (state.accounts.isEmpty()) {
                Text(
                    text = "Cadastre uma conta para registrar o pagamento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            state.accounts.forEach { account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.payInvoice(account.id)
                            showPaySheet = false
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryAvatar(colorHex = account.colorHex, iconKey = account.iconKey, size = 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InvoiceHeaderCard(
    colorHex: String,
    invoiceTotal: Double,
    statusLabel: String,
    periodInfo: String,
    modifier: Modifier = Modifier,
) {
    val dark = LocalIsDarkTheme.current
    val accent = colorFromHex(colorHex)

    val gradient = if (dark) {
        Brush.linearGradient(listOf(Color(0xFF221F2B), Color(0xFF121016)))
    } else {
        Brush.linearGradient(listOf(accent, shade(accent, 0.78f)))
    }
    val onCard = when {
        dark -> Color.White
        accent.luminance() > 0.55f -> Color(0xFF131A20)
        else -> Color.White
    }
    val onCardMuted = onCard.copy(alpha = 0.78f)

    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (dark) {
                    Modifier.shadow(10.dp, shape, clip = false, ambientColor = Color(0xFF0B0A0E), spotColor = Color(0xFF0B0A0E))
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(gradient)
            .then(
                if (dark) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x33FFFFFF), Color(0x11000000), Color(0x40000000)),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            )
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = "Fatura • $statusLabel",
                style = MaterialTheme.typography.titleSmall,
                color = onCardMuted,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Money.format(invoiceTotal),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = onCard,
            )
            if (periodInfo.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = periodInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCardMuted,
                )
            }
        }
    }
}

private fun statusLabel(status: InvoiceStatus): String = when (status) {
    InvoiceStatus.OPEN -> "Aberta"
    InvoiceStatus.CLOSED -> "Fechada"
    InvoiceStatus.PAID -> "Paga"
    InvoiceStatus.EMPTY -> "Sem lançamentos"
}

private fun periodInfo(state: CardDetailUiState): String {
    val cycle = state.cycle ?: return ""
    return "Fecha ${cycle.closingDate.ddMM()} • Vence ${cycle.dueDate.ddMM()}"
}

private fun LocalDate.ddMM(): String = "%02d/%02d".format(dayOfMonth, monthValue)

private fun shade(color: Color, factor: Float): Color =
    Color(
        red = color.red * factor,
        green = color.green * factor,
        blue = color.blue * factor,
        alpha = color.alpha,
    )
