package com.finzen.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neoRaised(
    base: Color,
    highlight: Color,
    shadow: Color,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 10.dp,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = shadow,
            spotColor = shadow,
        )
        .clip(shape)
        .background(base)
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(highlight, base, shadow),
                start = Offset.Zero,
                end = Offset.Infinite,
            ),
            shape = shape,
        )
}

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalIsDarkTheme.current) {
        val colors = FinanceTheme.colors
        Column(
            modifier = modifier.neoRaised(
                base = colors.neoBase,
                highlight = colors.neoHighlight,
                shadow = colors.neoShadow,
                cornerRadius = cornerRadius,
            ),
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            content = content,
        )
    }
}
