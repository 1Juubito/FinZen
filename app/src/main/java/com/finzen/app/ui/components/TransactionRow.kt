package com.finzen.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finzen.app.data.local.dao.TransactionDetails
import com.finzen.app.data.model.TransactionType
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money

@Composable
fun TransactionRow(
    item: TransactionDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val t = item.transaction
    val colors = FinanceTheme.colors
    val amountColor = when (t.type) {
        TransactionType.INCOME -> colors.income
        TransactionType.EXPENSE -> colors.expense
        TransactionType.TRANSFER -> colors.transfer
    }
    val prefix = when (t.type) {
        TransactionType.INCOME -> "+ "
        TransactionType.EXPENSE -> "- "
        TransactionType.TRANSFER -> ""
    }

    val subtitle = buildString {
        when (t.type) {
            TransactionType.TRANSFER -> {
                append(item.accountName ?: "?")
                if (item.toAccountName != null) append(" → ${item.toAccountName}")
            }
            else -> {
                append(item.categoryName ?: "Sem categoria")
                val source = item.cardName ?: item.accountName
                if (source != null) append(" • $source")
            }
        }
        if (t.installmentTotal != null && t.installmentNumber != null) {
            append("  ${t.installmentNumber}/${t.installmentTotal}")
        }
        if (showDate) append("  •  ${DateUtils.dayMonthLabel(t.date)}")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val visualColor = item.categoryColor ?: item.cardColor ?: item.accountColor
        val visualIcon = item.categoryIcon ?: if (t.creditCardId != null) "card" else "money"
        CategoryAvatar(colorHex = visualColor, iconKey = visualIcon)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = t.description.ifBlank { item.categoryName ?: "Transação" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = prefix + Money.formatAbs(t.amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
                maxLines = 1,
            )
            if (!t.isPaid) {
                Spacer(Modifier.height(4.dp))
                PendingPill()
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            trailing()
        }
    }
}
