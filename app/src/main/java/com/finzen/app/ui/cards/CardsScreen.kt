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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.model.CardWithUsage
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.EmptyState
import com.finzen.app.ui.icons.IconCatalog
import com.finzen.app.ui.theme.LocalIsDarkTheme
import com.finzen.app.ui.theme.colorFromHex
import com.finzen.app.util.Money

@Composable
fun CardsScreen(
    onAddCard: () -> Unit,
    onOpenCard: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardsViewModel = viewModel(factory = AppViewModelProvider.Factory),
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
                .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cartões",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAddCard) {
                Icon(Icons.Rounded.Add, contentDescription = "Adicionar cartão")
            }
        }

        if (state.cards.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.CreditCard,
                title = "Nenhum cartão",
                subtitle = "Toque no botão + para cadastrar seu primeiro cartão de crédito.",
                modifier = Modifier.padding(top = 40.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(state.cards, key = { it.card.id }) { item ->
                    CreditCardTile(
                        item = item,
                        onClick = { onOpenCard(item.card.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditCardTile(
    item: CardWithUsage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalIsDarkTheme.current
    val accent = colorFromHex(item.card.colorHex)

    val gradient = if (dark) {
        Brush.linearGradient(listOf(Color(0xFF221F2B), Color(0xFF121016)))
    } else {
        Brush.linearGradient(listOf(accent.copy(alpha = 1f), shade(accent, 0.78f)))
    }

    val accentBright = lerp(accent, Color.White, 0.30f)
    val onCard = when {
        dark -> Color.White
        accent.luminance() > 0.55f -> Color(0xFF131A20)
        else -> Color.White
    }
    val onCardMuted = onCard.copy(alpha = 0.78f)
    val iconTint = if (dark) accentBright else onCard
    val iconBg = if (dark) accentBright.copy(alpha = 0.20f) else onCard.copy(alpha = 0.18f)
    val progressFill = if (dark) accentBright else onCard
    val trackColor = onCard.copy(alpha = 0.22f)

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
            .clickable { onClick() }
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = IconCatalog.icon("card"),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.card.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = onCard,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.card.brand.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = onCardMuted,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Fatura atual",
                        style = MaterialTheme.typography.labelSmall,
                        color = onCardMuted,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = Money.format(item.currentInvoice),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = onCard,
                        maxLines = 1,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Limite disponível",
                        style = MaterialTheme.typography.labelSmall,
                        color = onCardMuted,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = Money.format(item.available),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = onCard,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(trackColor),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.usedFraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(progressFill),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${(item.usedFraction * 100).toInt()}% do limite de ${Money.format(item.card.creditLimit)}",
                style = MaterialTheme.typography.labelSmall,
                color = onCardMuted,
            )
        }
    }
}

private fun shade(color: Color, factor: Float): Color =
    Color(
        red = color.red * factor,
        green = color.green * factor,
        blue = color.blue * factor,
        alpha = color.alpha,
    )
