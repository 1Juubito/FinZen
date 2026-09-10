package com.finzen.app.ui.debts

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.local.entity.PersonalDebtEntity
import com.finzen.app.data.model.DebtDirection
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CompactDatePickerDialog
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.components.FilterChipPill
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    onBack: () -> Unit,
    onOpenPayoff: () -> Unit,
    onOpenReceivables: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<DebtEditorTarget?>(null) }

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
                text = "Dívidas",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenPayoff) {
                Icon(Icons.Rounded.Checklist, contentDescription = "Plano de quitação")
            }
            IconButton(onClick = { editor = DebtEditorTarget.New }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nova dívida")
            }
        }

        if (state.isEmpty) {
            SummaryRow(state, onOpenReceivables, Modifier.padding(horizontal = 16.dp))
            EmptyState(
                icon = Icons.Rounded.Group,
                title = "Nenhuma dívida",
                subtitle = "Anote quem te deve ou a quem você deve, com data e quitação.",
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item("summary") { SummaryRow(state, onOpenReceivables) }

                if (state.theyOwe.isNotEmpty()) {
                    item("h1") { SectionLabel("Me devem") }
                    items(state.theyOwe, key = { "t${it.id}" }) { debt ->
                        DebtRow(debt, FinanceTheme.colors.income) { editor = DebtEditorTarget.Edit(debt) }
                    }
                }
                if (state.iOwe.isNotEmpty()) {
                    item("h2") { SectionLabel("Eu devo") }
                    items(state.iOwe, key = { "i${it.id}" }) { debt ->
                        DebtRow(debt, FinanceTheme.colors.expense) { editor = DebtEditorTarget.Edit(debt) }
                    }
                }
                if (state.settled.isNotEmpty()) {
                    item("h3") { SectionLabel("Quitadas") }
                    items(state.settled, key = { "s${it.id}" }) { debt ->
                        DebtRow(debt, MaterialTheme.colorScheme.onSurfaceVariant, faded = true) {
                            editor = DebtEditorTarget.Edit(debt)
                        }
                    }
                }
            }
        }
    }

    val target = editor
    if (target != null) {
        DebtEditorSheet(
            target = target,
            onDismiss = { editor = null },
            onSave = { viewModel.save(it); editor = null },
            onSettle = { id, settled -> viewModel.setSettled(id, settled); editor = null },
            onDelete = { viewModel.delete(it); editor = null },
        )
    }
}

@Composable
private fun SummaryRow(state: DebtsUiState, onMeDevem: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryPill("Me devem", state.theyOweTotal, FinanceTheme.colors.income, Modifier.weight(1f), onClick = onMeDevem)
        SummaryPill("Eu devo", state.iOweTotal, FinanceTheme.colors.expense, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryPill(label: String, value: Double, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    NeoCard(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(
                text = Money.format(value),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
    )
}

@Composable
private fun DebtRow(
    debt: PersonalDebtEntity,
    color: Color,
    faded: Boolean = false,
    onClick: () -> Unit,
) {
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = debt.person.trim().firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = color,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = debt.person,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(DateUtils.dayMonthLabel(debt.date))
                        if (!debt.notes.isNullOrBlank()) append(" • ${debt.notes}")
                        if (debt.settled) append(" • quitada")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = Money.format(debt.amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (faded) MaterialTheme.colorScheme.onSurfaceVariant else color,
            )
        }
    }
}

private sealed interface DebtEditorTarget {
    data object New : DebtEditorTarget
    data class Edit(val debt: PersonalDebtEntity) : DebtEditorTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtEditorSheet(
    target: DebtEditorTarget,
    onDismiss: () -> Unit,
    onSave: (PersonalDebtEntity) -> Unit,
    onSettle: (id: Long, settled: Boolean) -> Unit,
    onDelete: (PersonalDebtEntity) -> Unit,
) {
    val existing = (target as? DebtEditorTarget.Edit)?.debt
    var direction by remember { mutableStateOf(existing?.direction ?: DebtDirection.THEY_OWE_ME) }
    var person by remember { mutableStateOf(existing?.person ?: "") }
    var digits by remember {
        mutableStateOf(existing?.let { Math.round(it.amount * 100).coerceAtLeast(0).toString() } ?: "")
    }
    var date by remember { mutableStateOf(existing?.date ?: DateUtils.now()) }
    var dueDate by remember { mutableStateOf(existing?.dueDate) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var keypadVisible by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showDueDate by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val amount = Money.centsToValue(digits)
    val canSave = person.isNotBlank() && amount > 0.0
    val accent = if (direction == DebtDirection.THEY_OWE_ME) FinanceTheme.colors.income else FinanceTheme.colors.expense

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = if (existing != null) "Editar dívida" else "Nova dívida",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChipPill("Me devem", direction == DebtDirection.THEY_OWE_ME, {
                    direction = DebtDirection.THEY_OWE_ME
                }, selectedColor = FinanceTheme.colors.income)
                FilterChipPill("Eu devo", direction == DebtDirection.I_OWE, {
                    direction = DebtDirection.I_OWE
                }, selectedColor = FinanceTheme.colors.expense)
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { focusManager.clearFocus(); keypadVisible = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Column {
                    Text("Valor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Money.formatCentsInput(digits),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = person,
                onValueChange = { person = it },
                label = { Text(if (direction == DebtDirection.THEY_OWE_ME) "Quem te deve" else "A quem você deve") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) keypadVisible = false },
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { focusManager.clearFocus(); keypadVisible = false; showDate = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = DateUtils.fullDateLabel(date),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { focusManager.clearFocus(); keypadVisible = false; showDueDate = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = dueDate?.let { "Vence em ${DateUtils.fullDateLabel(it)}" } ?: "Vencimento (opcional)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (dueDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (dueDate != null) {
                    Text(
                        text = "Remover",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { dueDate = null }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observação (opcional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) keypadVisible = false },
            )

            if (keypadVisible) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "Concluir",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { keypadVisible = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                DebtKeypad(
                    onDigit = { d -> digits = (digits + d).trimStart('0').take(12) },
                    onBackspace = { digits = digits.dropLast(1) },
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onSave(
                        (existing ?: PersonalDebtEntity(person = "", amount = 0.0, direction = direction, date = date)).copy(
                            person = person.trim(),
                            amount = amount,
                            direction = direction,
                            date = date,
                            dueDate = dueDate,
                            notes = notes.ifBlank { null },
                        ),
                    )
                },
                enabled = canSave,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text("Salvar", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }

            if (existing != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onSettle(existing.id, !existing.settled) },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text(if (existing.settled) "Reabrir" else "Marcar como quitada")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onDelete(existing) },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text("Excluir", color = FinanceTheme.colors.expense)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDate) {
        CompactDatePickerDialog(
            initialMillis = date,
            onConfirm = { date = it },
            onDismiss = { showDate = false },
            accent = accent,
        )
    }

    if (showDueDate) {
        CompactDatePickerDialog(
            initialMillis = dueDate ?: date,
            onConfirm = { dueDate = it },
            onDismiss = { showDueDate = false },
            accent = accent,
        )
    }
}

@Composable
private fun DebtKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
) {
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
