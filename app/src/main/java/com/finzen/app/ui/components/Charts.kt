package com.finzen.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DonutSlice(val value: Float, val color: Color)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier.size(190.dp),
    strokeWidth: Dp = 26.dp,
    gapDegrees: Float = 3f,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val stroke = Stroke(width = sw, cap = StrokeCap.Round)
            val diameter = size.minDimension - sw
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            if (total <= 0f) {
                drawArc(
                    color = trackColor, startAngle = 0f, sweepAngle = 360f,
                    useCenter = false, topLeft = topLeft, size = arcSize, style = stroke,
                )
            } else {
                var start = -90f
                slices.filter { it.value > 0f }.forEach { slice ->
                    val sweep = 360f * (slice.value / total)
                    drawArc(
                        color = slice.color,
                        startAngle = start + gapDegrees / 2f,
                        sweepAngle = (sweep - gapDegrees).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                    start += sweep
                }
            }
        }
        content()
    }
}

data class MonthBarData(val label: String, val income: Float, val expense: Float)

@Composable
fun IncomeExpenseBars(
    data: List<MonthBarData>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier,
    barMaxHeight: Dp = 132.dp,
) {
    val maxValue = (data.flatMap { listOf(it.income, it.expense) }.maxOrNull() ?: 0f)
        .coerceAtLeast(1f)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        data.forEach { d ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Bar(barMaxHeight * (d.income / maxValue), incomeColor)
                    Bar(barMaxHeight * (d.expense / maxValue), expenseColor)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = d.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Bar(height: Dp, color: Color) {
    Box(
        Modifier
            .width(13.dp)
            .height(height.coerceAtLeast(4.dp))
            .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
            .background(color),
    )
}

data class LinePoint(val label: String, val value: Float)

@Composable
fun LineChart(
    points: List<LinePoint>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    valueLabel: (Float) -> String = { it.toInt().toString() },
) {
    val maxValue = (points.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(1f)
    Row(modifier = modifier.fillMaxWidth().height(height)) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            AxisLabel(valueLabel(maxValue))
            AxisLabel(valueLabel(maxValue / 2f))
            AxisLabel(valueLabel(0f))
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (points.isEmpty()) return@Canvas
                val n = points.size
                val w = size.width
                val h = size.height
                val dx = if (n > 1) w / (n - 1) else 0f
                fun point(i: Int): Offset {
                    val x = if (n == 1) w / 2f else dx * i
                    val y = h - (points[i].value / maxValue) * h
                    return Offset(x, y.coerceIn(0f, h))
                }

                val line = Path()
                val fill = Path()
                val first = point(0)
                line.moveTo(first.x, first.y)
                fill.moveTo(first.x, h)
                fill.lineTo(first.x, first.y)
                for (i in 1 until n) {
                    val prev = point(i - 1)
                    val cur = point(i)
                    val midX = (prev.x + cur.x) / 2f
                    line.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                    fill.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                }
                val last = point(n - 1)
                fill.lineTo(last.x, h)
                fill.close()

                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)),
                )
                drawPath(
                    path = line,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            XAxisLabels(points)
        }
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun XAxisLabels(points: List<LinePoint>) {
    if (points.isEmpty()) return
    val n = points.size
    val indices = when {
        n <= 4 -> (0 until n).toList()
        else -> listOf(0, n / 3, 2 * n / 3, n - 1).distinct()
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        indices.forEach { i -> AxisLabel(points[i].label) }
    }
}

@Composable
fun SavingsGauge(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier.size(150.dp),
    strokeWidth: Dp = 16.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val stroke = Stroke(width = sw, cap = StrokeCap.Round)
            val diameter = size.minDimension - sw
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val startAngle = 135f
            val maxSweep = 270f
            drawArc(
                color = trackColor, startAngle = startAngle, sweepAngle = maxSweep,
                useCenter = false, topLeft = topLeft, size = arcSize, style = stroke,
            )
            drawArc(
                color = color, startAngle = startAngle,
                sweepAngle = maxSweep * progress.coerceIn(0f, 1f),
                useCenter = false, topLeft = topLeft, size = arcSize, style = stroke,
            )
        }
        content()
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    value: String,
    percent: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(color = color, size = 12.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = percent,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
