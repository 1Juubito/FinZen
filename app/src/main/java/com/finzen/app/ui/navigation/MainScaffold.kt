package com.finzen.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finzen.app.data.model.TransactionType
import com.finzen.app.notifications.AppNavBus
import com.finzen.app.notifications.TransactionPrefillBus
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.LocalIsDarkTheme

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.DASHBOARD, "Início", Icons.Rounded.Home),
    TabItem(Routes.TRANSACTIONS, "Transações", Icons.Rounded.ReceiptLong),
    TabItem(Routes.CARDS, "Cartões", Icons.Rounded.CreditCard),
    TabItem(Routes.MORE, "Mais", Icons.Rounded.GridView),
)

private val investTab = TabItem(Routes.INVESTMENTS, "Investir", Icons.Rounded.ShowChart)

private val topLevelRoutes = (tabs + investTab).map { it.route }.toSet()

@Composable
fun MainScaffold(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBars = currentRoute in topLevelRoutes

    val pendingPrefill by TransactionPrefillBus.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingPrefill) {
        pendingPrefill?.let { navController.navigate(Routes.addTransaction(type = it.type)) }
    }

    val pendingRoute by AppNavBus.route.collectAsStateWithLifecycle()
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let {
            navController.navigate(it)
            AppNavBus.consume()
        }
    }

    var quickAddOpen by remember { mutableStateOf(false) }
    val fabRotation by animateFloatAsState(
        targetValue = if (quickAddOpen && showBars) 45f else 0f,
        label = "fabRotation",
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBars) {
                FinBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        quickAddOpen = false
                        navController.navigateTab(route)
                    },
                )
            }
        },
        floatingActionButton = {
            if (showBars) {

                val dark = LocalIsDarkTheme.current
                FloatingActionButton(
                    onClick = { quickAddOpen = !quickAddOpen },
                    containerColor = if (dark) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.primary,
                    contentColor = if (dark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = if (quickAddOpen) "Fechar" else "Adicionar transação",
                        modifier = Modifier.rotate(fabRotation),
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            FinanceNavHost(navController = navController, modifier = Modifier.fillMaxSize())

            QuickAddMenu(
                expanded = quickAddOpen && showBars,
                onDismiss = { quickAddOpen = false },
                onPick = { type ->
                    quickAddOpen = false
                    navController.navigate(Routes.addTransaction(type = type))
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun QuickAddMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPick: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
        )
    }

    AnimatedVisibility(
        visible = expanded,
        modifier = modifier.padding(bottom = 84.dp),
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickAddPill("Receita", Icons.Rounded.TrendingUp, FinanceTheme.colors.income) {
                onPick(TransactionType.INCOME)
            }
            QuickAddPill("Despesa", Icons.Rounded.TrendingDown, FinanceTheme.colors.expense) {
                onPick(TransactionType.EXPENSE)
            }
            QuickAddPill("Transferência", Icons.Rounded.SwapHoriz, FinanceTheme.colors.transfer) {
                onPick(TransactionType.TRANSFER)
            }
        }
    }
}

@Composable
private fun QuickAddPill(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 18.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FinBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab(tabs[0], currentRoute, onSelect, Modifier.weight(1f))
            BottomTab(tabs[1], currentRoute, onSelect, Modifier.weight(1f))

            BottomTab(investTab, currentRoute, onSelect, Modifier.weight(1f))
            BottomTab(tabs[2], currentRoute, onSelect, Modifier.weight(1f))
            BottomTab(tabs[3], currentRoute, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomTab(
    tab: TabItem,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = currentRoute == tab.route
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable { onSelect(tab.route) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = color)
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = color,
        )
    }
}
