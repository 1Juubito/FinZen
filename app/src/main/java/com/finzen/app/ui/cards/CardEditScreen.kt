package com.finzen.app.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.model.CardBrand
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.FilterChipPill
import com.finzen.app.ui.icons.PaletteColors
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.colorFromHex

@Composable
fun CardEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                text = if (state.isEditing) "Editar cartão" else "Novo cartão",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (state.isEditing) {
                IconButton(onClick = viewModel::delete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Excluir",
                        tint = FinanceTheme.colors.expense,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(2.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Nome do cartão") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.limitInput,
                onValueChange = viewModel::setLimit,
                label = { Text("Limite (R$)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            DayStepperRow(
                label = "Dia de fechamento",
                value = state.closingDay,
                onChange = viewModel::setClosingDay,
            )

            DayStepperRow(
                label = "Dia de vencimento",
                value = state.dueDay,
                onChange = viewModel::setDueDay,
            )

            Column {
                FieldLabel("Cor")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(PaletteColors.swatches) { hex ->
                        ColorSwatch(
                            hex = hex,
                            selected = hex.equals(state.colorHex, ignoreCase = true),
                            onClick = { viewModel.setColor(hex) },
                        )
                    }
                }
            }

            Column {
                FieldLabel("Bandeira")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CardBrand.values()) { brand ->
                        FilterChipPill(
                            label = brand.label,
                            selected = brand == state.brand,
                            onClick = { viewModel.setBrand(brand) },
                        )
                    }
                }
            }

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
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun DayStepperRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            FieldLabel(label)
            Text(
                text = "Entre 1 e 28",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StepperButton(
            icon = Icons.Rounded.Remove,
            enabled = value > 1,
            contentDescription = "Diminuir",
            onClick = { onChange(value - 1) },
        )
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        StepperButton(
            icon = Icons.Rounded.Add,
            enabled = value < 28,
            contentDescription = "Aumentar",
            onClick = { onChange(value + 1) },
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ColorSwatch(
    hex: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = colorFromHex(hex)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Selecionada",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
