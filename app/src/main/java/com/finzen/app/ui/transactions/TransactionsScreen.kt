package com.finzen.app.ui.transactions

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.model.TransactionType
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CategoryAvatar
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.components.FilterChipPill
import com.finzen.app.ui.components.ListDivider
import com.finzen.app.ui.components.MonthNavigator
import com.finzen.app.ui.components.TransactionRow
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onOpenTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Transações",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (state.searching) {
                Text(
                    text = "${state.resultCount} resultado${if (state.resultCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MonthNavigator(
                    label = state.monthRef.shortLabel + " " + state.monthRef.year,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                )
            }
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Buscar por descrição, categoria…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryPill(
                if (state.pendingOnly) "A receber" else "Receitas",
                Money.format(state.income),
                FinanceTheme.colors.income,
                Modifier.weight(1f),
            )
            SummaryPill(
                if (state.pendingOnly) "A pagar" else "Despesas",
                Money.format(state.expense),
                FinanceTheme.colors.expense,
                Modifier.weight(1f),
            )

            val saldo = if (state.accountId != null || !state.hasActiveFilters) {
                state.accumulatedBalance
            } else {
                state.balance
            }
            SummaryPill(
                "Saldo",
                Money.format(saldo),
                if (saldo < 0) FinanceTheme.colors.expense else FinanceTheme.colors.income,
                Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChipPill("Tudo", state.filter == null, { viewModel.setFilter(null) })
            FilterChipPill(
                "Receitas",
                state.filter == TransactionType.INCOME,
                { viewModel.setFilter(TransactionType.INCOME) },
                selectedColor = FinanceTheme.colors.income,
            )
            FilterChipPill(
                "Despesas",
                state.filter == TransactionType.EXPENSE,
                { viewModel.setFilter(TransactionType.EXPENSE) },
                selectedColor = FinanceTheme.colors.expense,
            )
            FilterChipPill(
                "Pendentes",
                state.pendingOnly,
                { viewModel.setPendingOnly(!state.pendingOnly) },
            )
            CategoryFilterChip(
                label = state.selectedCategory?.name ?: "Categoria",
                active = state.categoryId != null,
                onClick = { showCategorySheet = true },
            )
            AccountFilterChip(
                label = state.selectedAccount?.name ?: "Conta",
                active = state.accountId != null,
                onClick = { showAccountSheet = true },
            )
            if (state.hasActiveFilters) {
                Text(
                    text = "Limpar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { viewModel.clearFilters() }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }

        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Rounded.ReceiptLong,
                title = when {
                    state.pendingOnly -> "Sem pendências"
                    state.hasActiveFilters -> "Nenhum resultado"
                    else -> "Nada por aqui"
                },
                subtitle = when {
                    state.pendingOnly -> "Tudo em dia! Nenhuma conta pendente neste mês."
                    state.hasActiveFilters -> "Nenhuma transação corresponde aos filtros aplicados."
                    else -> "Você ainda não tem transações em ${state.monthRef.label}."
                },
                modifier = Modifier.padding(top = 40.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                state.groups.forEach { group ->
                    item(key = group.dateMillis) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = group.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = Money.formatSigned(group.dayResult),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (group.dayResult < 0) FinanceTheme.colors.expense else FinanceTheme.colors.income,
                                )
                            }
                            NeoCard(
                            ) {
                                Column(Modifier.padding(vertical = 4.dp)) {
                                    group.items.forEachIndexed { index, item ->
                                        TransactionRow(
                                            item = item,
                                            onClick = { onOpenTransaction(item.transaction.id) },
                                            showDate = state.searching,
                                            trailing = when {
                                                !state.pendingOnly -> null

                                                item.transaction.creditCardId != null -> {
                                                    {
                                                        Text(
                                                            text = "Fatura",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(end = 8.dp),
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    {
                                                        IconButton(onClick = { viewModel.markPaid(item.transaction.id) }) {
                                                            Icon(
                                                                Icons.Rounded.CheckCircle,
                                                                contentDescription = "Marcar como pago",
                                                                tint = FinanceTheme.colors.income,
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                        )
                                        if (index < group.items.lastIndex) ListDivider(72.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showCategorySheet = false }, sheetState = sheetState) {
            Text(
                text = "Filtrar por categoria",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                CategorySheetRow(
                    label = "Todas as categorias",
                    colorHex = null,
                    iconKey = null,
                    selected = state.categoryId == null,
                ) {
                    viewModel.setCategory(null)
                    showCategorySheet = false
                }
                state.categories.forEach { category ->
                    CategorySheetRow(
                        label = category.name,
                        colorHex = category.colorHex,
                        iconKey = category.iconKey,
                        selected = state.categoryId == category.id,
                    ) {
                        viewModel.setCategory(category.id)
                        showCategorySheet = false
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAccountSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showAccountSheet = false }, sheetState = sheetState) {
            Text(
                text = "Filtrar por conta",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                CategorySheetRow(
                    label = "Contas principais",
                    colorHex = null,
                    iconKey = null,
                    selected = state.accountId == null,
                ) {
                    viewModel.setAccount(null)
                    showAccountSheet = false
                }
                state.accounts.forEach { account ->
                    CategorySheetRow(
                        label = account.name,
                        colorHex = account.colorHex,
                        iconKey = account.iconKey,
                        selected = state.accountId == account.id,
                    ) {
                        viewModel.setAccount(account.id)
                        showAccountSheet = false
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AccountFilterChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val container = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        modifier = Modifier.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Wallet, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val container = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        modifier = Modifier.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Category, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
        }
    }
}

@Composable
private fun CategorySheetRow(
    label: String,
    colorHex: String?,
    iconKey: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(colorHex = colorHex, iconKey = iconKey, size = 38.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1,
        )
    }
}
