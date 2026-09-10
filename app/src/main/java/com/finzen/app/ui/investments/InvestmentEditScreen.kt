package com.finzen.app.ui.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.model.InvestmentType
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CompactDatePickerDialog
import com.finzen.app.ui.components.FilterChipPill
import com.finzen.app.ui.components.NeoField
import com.finzen.app.ui.icons.IconCatalog
import com.finzen.app.ui.icons.PaletteColors
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.colorFromHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvestmentEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accentColor = colorFromHex(state.colorHex)
    var showDatePicker by remember { mutableStateOf(false) }

    var keypadVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
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
                text = if (state.isEditing) "Editar investimento" else "Novo investimento",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (state.isEditing) {
                IconButton(onClick = viewModel::delete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = FinanceTheme.colors.expense)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(2.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = IconCatalog.icon(state.type.iconKey),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(38.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = state.name.ifBlank { "Nome do investimento" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (state.name.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = state.type.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            NeoField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = "Nome do investimento",
                singleLine = true,
                onFocusChanged = { focused -> if (focused) keypadVisible = false },
            )

            FieldLabel("Tipo")
            TypePicker(
                selected = state.type,
                accent = accentColor,
                onSelect = viewModel::setType,
            )

            NeoField(
                value = state.institution,
                onValueChange = viewModel::setInstitution,
                label = "Instituição / corretora",
                singleLine = true,
                onFocusChanged = { focused -> if (focused) keypadVisible = false },
            )

            FieldLabel("Valores")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AmountBox(
                    label = "Aplicado",
                    value = state.investedFormatted,
                    selected = state.editingField == AmountField.INVESTED,
                    accent = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.selectField(AmountField.INVESTED)
                        focusManager.clearFocus()
                        keypadVisible = true
                    },
                )
                AmountBox(
                    label = "Saldo atual",
                    value = state.currentFormatted,
                    selected = state.editingField == AmountField.CURRENT,
                    accent = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.selectField(AmountField.CURRENT)
                        focusManager.clearFocus()
                        keypadVisible = true
                    },
                )
            }
            if (keypadVisible) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { keypadVisible = false }) {
                        Text(
                            text = "Concluir",
                            color = accentColor,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
                AmountKeypad(
                    onDigit = viewModel::appendDigit,
                    onBackspace = viewModel::backspace,
                )
            }

            if (!state.isEditing && state.accounts.isNotEmpty()) {
                FieldLabel("De qual conta saiu? (opcional)")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.accounts.forEach { acc ->
                        FilterChipPill(
                            label = acc.name,
                            selected = state.sourceAccountId == acc.id,
                            onClick = { viewModel.setSourceAccount(acc.id) },
                            selectedColor = accentColor,
                        )
                    }
                    FilterChipPill(
                        label = "De fora",
                        selected = state.sourceAccountId == null,
                        onClick = { viewModel.setSourceAccount(null) },
                    )
                }
            }

            FieldLabel("Data de aplicação")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { showDatePicker = true }
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
                    text = state.dateLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state.date != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f),
                )
                if (state.date != null) {
                    IconButton(onClick = viewModel::clearDate, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Remover data",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            FieldLabel("Cor")
            ColorPicker(
                selected = state.colorHex,
                onSelect = viewModel::selectColor,
            )

            NeoField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = "Observações",
                onFocusChanged = { focused -> if (focused) keypadVisible = false },
            )

            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = viewModel::save,
            enabled = state.canSave,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(54.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Salvar",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
        }
    }

    if (showDatePicker) {
        CompactDatePickerDialog(
            initialMillis = state.date,
            onConfirm = { viewModel.setDate(it) },
            onDismiss = { showDatePicker = false },
            onClear = { viewModel.setDate(null) },
            accent = accentColor,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypePicker(
    selected: InvestmentType,
    accent: Color,
    onSelect: (InvestmentType) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InvestmentType.entries.forEach { type ->
            FilterChipPill(
                label = type.label,
                selected = type == selected,
                onClick = { onSelect(type) },
                selectedColor = accent,
            )
        }
    }
}

@Composable
private fun AmountBox(
    label: String,
    value: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (selected) Modifier.border(2.dp, accent, MaterialTheme.shapes.medium) else Modifier,
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ColorPicker(
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(PaletteColors.swatches) { hex ->
            val color = colorFromHex(hex)
            val isSelected = hex.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selecionada",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "<"),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->

                    val keyColor = if (key == "<") {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(keyColor)
                            .clickable {
                                when (key) {
                                    "<" -> onBackspace()
                                    "000" -> repeat(3) { onDigit('0') }
                                    else -> onDigit(key.first())
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (key == "<") "⌫" else key,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
