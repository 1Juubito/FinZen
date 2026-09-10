package com.finzen.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.finzen.app.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

private val weekdayLabels = listOf("D", "S", "T", "Q", "Q", "S", "S")

private fun monthCells(ym: YearMonth): List<Int?> {
    val offset = ym.atDay(1).dayOfWeek.value % 7
    val days = ym.lengthOfMonth()
    return List(42) { i ->
        val d = i - offset + 1
        if (d in 1..days) d else null
    }
}

@Composable
fun CompactDatePickerDialog(
    initialMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val today = remember { LocalDate.now() }
    val initialDate = remember { initialMillis?.let { DateUtils.toLocalDate(it) } ?: today }
    var selected by remember { mutableStateOf(initialDate) }
    var shown by remember { mutableStateOf(YearMonth.from(initialDate)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { shown = shown.minusMonths(1) }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Mês anterior")
                    }
                    Text(
                        text = DateUtils.monthYearLabel(shown.year, shown.monthValue),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { shown = shown.plusMonths(1) }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Próximo mês")
                    }
                }
                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth()) {
                    weekdayLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))

                val cells = monthCells(shown)
                for (week in 0 until 6) {
                    Row(Modifier.fillMaxWidth()) {
                        for (dow in 0 until 7) {
                            DayCell(
                                day = cells[week * 7 + dow],
                                month = shown,
                                selected = selected,
                                today = today,
                                accent = accent,
                                modifier = Modifier.weight(1f),
                                onPick = { selected = it },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (onClear != null) {
                        TextButton(onClick = { onClear(); onDismiss() }) { Text("Limpar") }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    TextButton(onClick = {
                        onConfirm(DateUtils.localDateToMillis(selected))
                        onDismiss()
                    }) { Text("OK") }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int?,
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    accent: Color,
    modifier: Modifier = Modifier,
    onPick: (LocalDate) -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (day != null) {
            val date = month.atDay(day)
            val isSelected = date == selected
            val isToday = date == today
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .then(
                        when {
                            isSelected -> Modifier.background(accent)
                            isToday -> Modifier.border(1.dp, accent, CircleShape)
                            else -> Modifier
                        },
                    )
                    .clickable { onPick(date) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
