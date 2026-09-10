package com.finzen.app.ui.goals

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
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
import com.finzen.app.data.local.entity.GoalEntity
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CategoryAvatar
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.ui.theme.colorFromHex
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money
import kotlin.math.min

@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var fundsTarget by remember { mutableStateOf<GoalEntity?>(null) }

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
                text = "Metas",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAddGoal) {
                Icon(Icons.Rounded.Add, contentDescription = "Nova meta")
            }
        }

        if (state.goals.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Savings,
                title = "Nenhuma meta",
                subtitle = "Crie metas de economia e acompanhe seu progresso.",
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "summary") {
                    GoalsSummaryCard(
                        totalSaved = state.totalSaved,
                        totalTarget = state.totalTarget,
                    )
                }
                items(state.goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onClick = { onEditGoal(goal.id) },
                        onAddFunds = { fundsTarget = goal },
                    )
                }
            }
        }
    }

    val target = fundsTarget
    if (target != null) {
        AddFundsSheet(
            goal = target,
            onDismiss = { fundsTarget = null },
            onConfirm = { amount ->
                viewModel.addFunds(target.id, amount)
                fundsTarget = null
            },
        )
    }
}

@Composable
private fun GoalsSummaryCard(
    totalSaved: Double,
    totalTarget: Double,
    modifier: Modifier = Modifier,
) {
    val fraction = if (totalTarget <= 0) 0f else (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f)
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "Total guardado",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = Money.format(totalSaved),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = FinanceTheme.colors.income,
                )
                Text(
                    text = " de ${Money.format(totalTarget)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(fraction = fraction, color = FinanceTheme.colors.income)
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    onClick: () -> Unit,
    onAddFunds: () -> Unit,
) {
    val barColor = if (goal.colorHex.isBlank()) FinanceTheme.colors.income else colorFromHex(goal.colorHex)
    val fraction = if (goal.targetAmount <= 0) {
        0f
    } else {
        (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    }
    val percent = (fraction * 100).toInt()

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar(colorHex = goal.colorHex, iconKey = goal.iconKey, size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${Money.format(goal.savedAmount)} de ${Money.format(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onAddFunds,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(barColor.copy(alpha = 0.16f)),
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Adicionar valor",
                        tint = barColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            ProgressBar(fraction = fraction, color = barColor)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = barColor,
                    modifier = Modifier.weight(1f),
                )
                if (goal.deadline != null) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Meta até ${DateUtils.fullDateLabel(goal.deadline)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFundsSheet(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit,
) {
    var digits by remember { mutableStateOf("") }
    var isWithdraw by remember { mutableStateOf(false) }
    val accent = if (goal.colorHex.isBlank()) FinanceTheme.colors.income else colorFromHex(goal.colorHex)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rawAmount = Money.centsToValue(digits)

    val effectiveAmount = if (isWithdraw) -min(rawAmount, goal.savedAmount) else rawAmount
    val canConfirm = rawAmount > 0.0 && (!isWithdraw || goal.savedAmount > 0.0)
    val displayColor = if (isWithdraw) FinanceTheme.colors.expense else accent

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ToggleOption(
                    label = "Adicionar",
                    icon = Icons.Rounded.Add,
                    selected = !isWithdraw,
                    selectedColor = FinanceTheme.colors.income,
                    modifier = Modifier.weight(1f),
                    onClick = { isWithdraw = false },
                )
                ToggleOption(
                    label = "Retirar",
                    icon = Icons.Rounded.Remove,
                    selected = isWithdraw,
                    selectedColor = FinanceTheme.colors.expense,
                    modifier = Modifier.weight(1f),
                    onClick = { isWithdraw = true },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (isWithdraw) "Valor a retirar" else "Valor a adicionar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Money.formatCentsInput(digits),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = displayColor,
                )
            }

            FundsKeypad(
                onDigit = { d -> digits = (digits + d).trimStart('0').take(12) },
                onBackspace = { digits = digits.dropLast(1) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Button(
                onClick = { onConfirm(effectiveAmount) },
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
private fun ToggleOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FundsKeypad(
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
