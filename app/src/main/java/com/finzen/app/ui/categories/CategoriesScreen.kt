package com.finzen.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.model.TransactionType
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CategoryAvatar
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.components.FilterChipPill
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onAddCategory: (TransactionType) -> Unit,
    onEditCategory: (Long, TransactionType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(TransactionType.EXPENSE) }

    val isExpense = selectedTab == TransactionType.EXPENSE
    val categories = if (isExpense) state.expense else state.income
    val accentColor = if (isExpense) FinanceTheme.colors.expense else FinanceTheme.colors.income

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
                text = "Categorias",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onAddCategory(selectedTab) }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nova categoria")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChipPill(
                label = "Despesas",
                selected = isExpense,
                onClick = { selectedTab = TransactionType.EXPENSE },
                selectedColor = FinanceTheme.colors.expense,
            )
            FilterChipPill(
                label = "Receitas",
                selected = !isExpense,
                onClick = { selectedTab = TransactionType.INCOME },
                selectedColor = FinanceTheme.colors.income,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (categories.isEmpty()) {
                item {
                    NeoCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        EmptyState(
                            icon = Icons.Rounded.Category,
                            title = if (isExpense) "Nenhuma categoria de despesa" else "Nenhuma categoria de receita",
                            subtitle = "Toque no botão + para criar uma nova categoria.",
                        )
                    }
                }
            } else {
                items(categories, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        onClick = { onEditCategory(category.id, selectedTab) },
                    )
                }
            }

            item {
                NeoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .clickable { onAddCategory(selectedTab) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = accentColor,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Nova categoria",
                            style = MaterialTheme.typography.titleSmall,
                            color = accentColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryAvatar(
                colorHex = category.colorHex,
                iconKey = category.iconKey,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
