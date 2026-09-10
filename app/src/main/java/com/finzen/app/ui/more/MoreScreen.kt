package com.finzen.app.ui.more

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finzen.app.ui.theme.NeoCard

private data class MoreItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun MoreScreen(
    onOpenAccounts: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenGoalProjection: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenCategoryRules: () -> Unit,
    onOpenInstallments: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenDebtPayoff: () -> Unit,
    onOpenReceivables: () -> Unit,
    onOpenAgenda: () -> Unit,
    onOpenForecast: () -> Unit,
    onOpenSpendable: () -> Unit,
    onOpenBurnRate: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        MoreItem("Contas", "Carteiras, bancos e investimentos", Icons.Rounded.AccountBalanceWallet, onOpenAccounts),
        MoreItem("Categorias", "Organize receitas e despesas", Icons.Rounded.Category, onOpenCategories),
        MoreItem("Lançamentos recorrentes", "Contas e receitas que se repetem", Icons.Rounded.Repeat, onOpenRecurring),
        MoreItem("Regras de categoria", "Categorize lançamentos automaticamente", Icons.Rounded.Label, onOpenCategoryRules),
        MoreItem("Planejamento", "Orçamentos por categoria", Icons.Rounded.PieChart, onOpenBudgets),
        MoreItem("Relatórios", "Gráficos e tendências", Icons.Rounded.BarChart, onOpenReports),
        MoreItem("Metas", "Objetivos de economia", Icons.Rounded.Flag, onOpenGoals),
        MoreItem("Projeção de metas", "Quando você bate cada meta, ou quanto guardar/mês", Icons.Rounded.Insights, onOpenGoalProjection),
        MoreItem("Agenda", "Tudo que vence: faturas, contas e dívidas", Icons.Rounded.EventNote, onOpenAgenda),
        MoreItem("Previsão", "Projeção do saldo dos próximos meses", Icons.Rounded.Timeline, onOpenForecast),
        MoreItem("Quanto posso gastar", "Quanto sobra livre para gastar este mês", Icons.Rounded.Savings, onOpenSpendable),
        MoreItem("Projeção pelo ritmo", "Onde o mês deve fechar no seu ritmo de gastos", Icons.Rounded.Speed, onOpenBurnRate),
        MoreItem("Saúde financeira", "Poupança, reserva e comprometimento da renda", Icons.Rounded.MonitorHeart, onOpenHealth),
        MoreItem("Parcelas futuras", "Compromissos das próximas faturas", Icons.Rounded.CreditCard, onOpenInstallments),
        MoreItem("Dívidas", "Quem te deve e a quem você deve", Icons.Rounded.Group, onOpenDebts),
        MoreItem("Plano de quitação", "Ordene e zere o que você deve, passo a passo", Icons.Rounded.Checklist, onOpenDebtPayoff),
        MoreItem("A receber", "Quem te deve, vencimentos e entradas previstas", Icons.Rounded.Payments, onOpenReceivables),
        MoreItem("Configurações", "Tema e dados do app", Icons.Rounded.Settings, onOpenSettings),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Text(
            text = "Mais",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items.size) { index ->
                val item = items[index]
                NeoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { item.onClick() },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Error404Saldo • Controle financeiro premium • build 24 • Neo Suave (escuro)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}
