package com.finzen.app.ui.budgets

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.model.BudgetProgress
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CategoryAvatar
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.components.MonthNavigator
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.ui.theme.colorFromHex
import com.finzen.app.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<BudgetEditorTarget?>(null) }

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
                text = "Planejamento",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { editor = BudgetEditorTarget.New }) {
                Icon(Icons.Rounded.Add, contentDescription = "Novo orçamento")
            }
        }

        MonthNavigator(
            label = state.monthRef.label,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Spacer(Modifier.height(8.dp))

        if (state.items.isEmpty()) {
            BudgetSummaryCard(
                totalBudget = state.totalBudget,
                totalSpent = state.totalSpent,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            EmptyState(
                icon = Icons.Rounded.PieChart,
                title = "Nenhum orçamento",
                subtitle = "Defina limites de gastos por categoria.",
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "summary") {
                    BudgetSummaryCard(
                        totalBudget = state.totalBudget,
                        totalSpent = state.totalSpent,
                    )
                }
                items(state.items, key = { it.category.id }) { progress ->
                    BudgetRow(
                        progress = progress,
                        onClick = { editor = BudgetEditorTarget.Edit(progress) },
                    )
                }
            }
        }
    }

    val target = editor
    if (target != null) {
        BudgetEditorSheet(
            target = target,
            expenseCategories = state.expenseCategories,
            onDismiss = { editor = null },
            onSave = { categoryId, amount ->
                viewModel.saveBudget(categoryId, amount)
                editor = null
            },
            onDelete = { categoryId ->
                viewModel.deleteBudget(categoryId)
                editor = null
            },
        )
    }
}

@Composable
private fun BudgetSummaryCard(
    totalBudget: Double,
    totalSpent: Double,
    modifier: Modifier = Modifier,
) {
    val isOver = totalSpent > totalBudget
    val fraction = if (totalBudget <= 0) 0f else (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f)
    NeoCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "Orçamento do mês",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = Money.format(totalSpent),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isOver) FinanceTheme.colors.expense else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = " de ${Money.format(totalBudget)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(
                fraction = fraction,
                color = if (isOver) FinanceTheme.colors.expense else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isOver) {
                    "Excedeu em ${Money.format(totalSpent - totalBudget)}"
                } else {
                    "Restam ${Money.format(totalBudget - totalSpent)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOver) FinanceTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BudgetRow(
    progress: BudgetProgress,
    onClick: () -> Unit,
) {
    val category = progress.category
    val barColor = if (progress.isOver) FinanceTheme.colors.expense else colorFromHex(category.colorHex)
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar(colorHex = category.colorHex, iconKey = category.iconKey, size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${Money.format(progress.spent)} de ${Money.format(progress.budget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (progress.isOver) "Excedeu" else "Restam ${Money.format(progress.remaining)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (progress.isOver) FinanceTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(fraction = progress.fraction, color = barColor)
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

private sealed interface BudgetEditorTarget {
    data object New : BudgetEditorTarget
    data class Edit(val progress: BudgetProgress) : BudgetEditorTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditorSheet(
    target: BudgetEditorTarget,
    expenseCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (categoryId: Long, amount: Double) -> Unit,
    onDelete: (categoryId: Long) -> Unit,
) {
    val isEditing = target is BudgetEditorTarget.Edit
    val lockedCategory = (target as? BudgetEditorTarget.Edit)?.progress?.category

    var selectedCategoryId by remember {
        mutableStateOf(lockedCategory?.id)
    }
    var digits by remember {
        mutableStateOf(
            when (target) {
                is BudgetEditorTarget.Edit ->
                    Math.round(target.progress.budget * 100).coerceAtLeast(0).toString()
                BudgetEditorTarget.New -> ""
            }
        )
    }

    var keypadVisible by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val amount = Money.centsToValue(digits)
    val canSave = selectedCategoryId != null && amount > 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Text(
                text = if (isEditing) "Editar orçamento" else "Novo orçamento",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { keypadVisible = true }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Limite mensal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Money.formatCentsInput(digits),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = "Categoria",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )

            if (isEditing && lockedCategory != null) {
                CategorySelectRow(
                    category = lockedCategory,
                    selected = true,
                    onClick = {},
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = if (keypadVisible) 130.dp else 260.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    expenseCategories.forEach { category ->
                        CategorySelectRow(
                            category = category,
                            selected = category.id == selectedCategoryId,
                            onClick = { selectedCategoryId = category.id },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (keypadVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { keypadVisible = false }) {
                        Text(
                            text = "Concluir",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
                BudgetKeypad(
                    onDigit = { d -> digits = (digits + d).trimStart('0').take(12) },
                    onBackspace = { digits = digits.dropLast(1) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }

            Button(
                onClick = { selectedCategoryId?.let { onSave(it, amount) } },
                enabled = canSave,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(54.dp),
            ) {
                Text(
                    text = "Salvar",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
            }

            if (isEditing && lockedCategory != null) {
                OutlinedButton(
                    onClick = { onDelete(lockedCategory.id) },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                ) {
                    Text(
                        text = "Remover",
                        color = FinanceTheme.colors.expense,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategorySelectRow(
    category: CategoryEntity,
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
        CategoryAvatar(colorHex = category.colorHex, iconKey = category.iconKey, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = category.name,
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
private fun BudgetKeypad(
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
