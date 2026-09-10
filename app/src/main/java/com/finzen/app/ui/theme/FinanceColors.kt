package com.finzen.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FinanceColors(
    val income: Color,
    val onIncomeContainer: Color,
    val incomeContainer: Color,
    val expense: Color,
    val onExpenseContainer: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val warning: Color,
    val heroGradient: List<Color>,
    val incomeGradient: List<Color>,
    val expenseGradient: List<Color>,

    val neoBase: Color,
    val neoHighlight: Color,
    val neoShadow: Color,
)

val LightFinanceColors = FinanceColors(
    income = Color(0xFF1FA463),
    onIncomeContainer = Color(0xFF053A22),
    incomeContainer = Color(0xFFD3F9DF),
    expense = Color(0xFFE03E44),
    onExpenseContainer = Color(0xFF5A1115),
    expenseContainer = Color(0xFFFFE0E1),
    transfer = Color(0xFF3B8ED0),
    warning = Color(0xFFF08C00),
    heroGradient = listOf(Color(0xFF9C3FE4), Color(0xFF820AD1), Color(0xFF5E0A96)),
    incomeGradient = listOf(Color(0xFF20C997), Color(0xFF12B886)),
    expenseGradient = listOf(Color(0xFFFF6B6B), Color(0xFFE03E44)),

    neoBase = Color(0xFFFFFFFF),
    neoHighlight = Color(0xFFFFFFFF),
    neoShadow = Color(0xFFD6DCE4),
)

val DarkFinanceColors = FinanceColors(
    income = Color(0xFF51CF66),
    onIncomeContainer = Color(0xFFD3F9DF),
    incomeContainer = Color(0xFF143A29),
    expense = Color(0xFFFF7A7A),
    onExpenseContainer = Color(0xFFFFD9D9),
    expenseContainer = Color(0xFF3B1B1E),
    transfer = Color(0xFF74C0FC),
    warning = Color(0xFFFFC078),
    heroGradient = listOf(Color(0xFF8B27D6), Color(0xFF6E0BB0), Color(0xFF4A0B78)),
    incomeGradient = listOf(Color(0xFF38D9A9), Color(0xFF12B886)),
    expenseGradient = listOf(Color(0xFFFF8787), Color(0xFFE03E44)),

    neoBase = Color(0xFF1D1C24),
    neoHighlight = Color(0xFF2B2934),
    neoShadow = Color(0xFF0E0D12),
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }
