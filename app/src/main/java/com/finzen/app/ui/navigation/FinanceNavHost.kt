package com.finzen.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.finzen.app.data.model.TransactionType
import com.finzen.app.ui.accounts.AccountEditScreen
import com.finzen.app.ui.accounts.AccountsScreen
import com.finzen.app.ui.agenda.AgendaScreen
import com.finzen.app.ui.budgets.BudgetsScreen
import com.finzen.app.ui.burnrate.BurnRateScreen
import com.finzen.app.ui.cards.CardDetailScreen
import com.finzen.app.ui.cards.CardEditScreen
import com.finzen.app.ui.cards.CardsScreen
import com.finzen.app.ui.categories.CategoriesScreen
import com.finzen.app.ui.categories.CategoryEditScreen
import com.finzen.app.ui.dashboard.DashboardScreen
import com.finzen.app.ui.debts.DebtPayoffScreen
import com.finzen.app.ui.debts.DebtsScreen
import com.finzen.app.ui.debts.ReceivablesScreen
import com.finzen.app.ui.forecast.CashFlowScreen
import com.finzen.app.ui.goals.GoalEditScreen
import com.finzen.app.ui.goals.GoalProjectionScreen
import com.finzen.app.ui.goals.GoalsScreen
import com.finzen.app.ui.health.FinancialHealthScreen
import com.finzen.app.ui.installments.InstallmentsScreen
import com.finzen.app.ui.investments.InvestmentEditScreen
import com.finzen.app.ui.investments.InvestmentsScreen
import com.finzen.app.ui.more.MoreScreen
import com.finzen.app.ui.recurring.RecurringScreen
import com.finzen.app.ui.reports.ReportsScreen
import com.finzen.app.ui.rules.CategoryRulesScreen
import com.finzen.app.ui.settings.SettingsScreen
import com.finzen.app.ui.spendable.SpendableScreen
import com.finzen.app.ui.transaction.AddEditTransactionScreen
import com.finzen.app.ui.transactions.TransactionsScreen

@Composable
fun FinanceNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier,
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onAddTransaction = { type -> navController.navigate(Routes.addTransaction(type = type)) },
                onOpenTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                onOpenReports = { navController.navigate(Routes.REPORTS) },
                onOpenTransaction = { id -> navController.navigate(Routes.addTransaction(txId = id)) },
                onOpenDebts = { navController.navigate(Routes.DEBTS) },
                onOpenCards = { navController.navigate(Routes.CARDS) },
            )
        }

        composable(Routes.TRANSACTIONS) {
            TransactionsScreen(
                onOpenTransaction = { id -> navController.navigate(Routes.addTransaction(txId = id)) },
            )
        }

        composable(Routes.CARDS) {
            CardsScreen(
                onAddCard = { navController.navigate(Routes.cardEdit()) },
                onOpenCard = { id -> navController.navigate(Routes.cardDetail(id)) },
            )
        }

        composable(Routes.MORE) {
            MoreScreen(
                onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                onOpenBudgets = { navController.navigate(Routes.BUDGETS) },
                onOpenReports = { navController.navigate(Routes.REPORTS) },
                onOpenGoals = { navController.navigate(Routes.GOALS) },
                onOpenGoalProjection = { navController.navigate(Routes.GOAL_PROJECTION) },
                onOpenRecurring = { navController.navigate(Routes.RECURRING) },
                onOpenCategoryRules = { navController.navigate(Routes.CATEGORY_RULES) },
                onOpenInstallments = { navController.navigate(Routes.INSTALLMENTS) },
                onOpenDebts = { navController.navigate(Routes.DEBTS) },
                onOpenDebtPayoff = { navController.navigate(Routes.DEBT_PAYOFF) },
                onOpenReceivables = { navController.navigate(Routes.RECEIVABLES) },
                onOpenAgenda = { navController.navigate(Routes.AGENDA) },
                onOpenForecast = { navController.navigate(Routes.FORECAST) },
                onOpenSpendable = { navController.navigate(Routes.SPENDABLE) },
                onOpenBurnRate = { navController.navigate(Routes.BURN_RATE) },
                onOpenHealth = { navController.navigate(Routes.FINANCIAL_HEALTH) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.ADD_TRANSACTION,
            arguments = listOf(
                navArgument(Routes.ARG_TX_ID) { type = NavType.LongType; defaultValue = -1L },
                navArgument(Routes.ARG_TX_TYPE) { type = NavType.StringType; defaultValue = TransactionType.EXPENSE.name },
                navArgument(Routes.ARG_CARD_ID) { type = NavType.LongType; defaultValue = -1L },
            ),
        ) {
            AddEditTransactionScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ACCOUNTS) {
            AccountsScreen(
                onBack = { navController.popBackStack() },
                onAddAccount = { navController.navigate(Routes.accountEdit()) },
                onEditAccount = { id -> navController.navigate(Routes.accountEdit(id)) },
            )
        }

        composable(
            route = Routes.ACCOUNT_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_ACCOUNT_ID) { type = NavType.LongType; defaultValue = -1L },
            ),
        ) {
            AccountEditScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.CARD_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_CARD_ID) { type = NavType.LongType }),
        ) {
            CardDetailScreen(
                onBack = { navController.popBackStack() },
                onEditCard = { id -> navController.navigate(Routes.cardEdit(id)) },
                onAddExpense = { cardId ->
                    navController.navigate(Routes.addTransaction(type = TransactionType.EXPENSE, cardId = cardId))
                },
                onOpenTransaction = { id -> navController.navigate(Routes.addTransaction(txId = id)) },
            )
        }

        composable(
            route = Routes.CARD_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_CARD_ID) { type = NavType.LongType; defaultValue = -1L },
            ),
        ) {
            CardEditScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                onBack = { navController.popBackStack() },
                onAddCategory = { type -> navController.navigate(Routes.categoryEdit(type = type)) },
                onEditCategory = { id, type -> navController.navigate(Routes.categoryEdit(id, type)) },
            )
        }

        composable(
            route = Routes.CATEGORY_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.LongType; defaultValue = -1L },
                navArgument(Routes.ARG_CATEGORY_TYPE) { type = NavType.StringType; defaultValue = TransactionType.EXPENSE.name },
            ),
        ) {
            CategoryEditScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.BUDGETS) {
            BudgetsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.GOALS) {
            GoalsScreen(
                onBack = { navController.popBackStack() },
                onAddGoal = { navController.navigate(Routes.goalEdit()) },
                onEditGoal = { id -> navController.navigate(Routes.goalEdit(id)) },
            )
        }

        composable(
            route = Routes.GOAL_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_GOAL_ID) { type = NavType.LongType; defaultValue = -1L },
            ),
        ) {
            GoalEditScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.INVESTMENTS) {
            InvestmentsScreen(
                onAddInvestment = { navController.navigate(Routes.investmentEdit()) },
                onEditInvestment = { id -> navController.navigate(Routes.investmentEdit(id)) },
            )
        }

        composable(
            route = Routes.INVESTMENT_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_INVESTMENT_ID) { type = NavType.LongType; defaultValue = -1L },
            ),
        ) {
            InvestmentEditScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.RECURRING) {
            RecurringScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CATEGORY_RULES) {
            CategoryRulesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.INSTALLMENTS) {
            InstallmentsScreen(
                onBack = { navController.popBackStack() },
                onOpenTransaction = { id -> navController.navigate(Routes.addTransaction(txId = id)) },
            )
        }

        composable(Routes.DEBTS) {
            DebtsScreen(
                onBack = { navController.popBackStack() },
                onOpenPayoff = { navController.navigate(Routes.DEBT_PAYOFF) },
                onOpenReceivables = { navController.navigate(Routes.RECEIVABLES) },
            )
        }

        composable(Routes.DEBT_PAYOFF) {
            DebtPayoffScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.RECEIVABLES) {
            ReceivablesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.AGENDA) {
            AgendaScreen(
                onBack = { navController.popBackStack() },
                onOpenTransaction = { id -> navController.navigate(Routes.addTransaction(txId = id)) },
                onOpenCard = { id -> navController.navigate(Routes.cardDetail(id)) },
                onOpenRecurring = { navController.navigate(Routes.RECURRING) },
                onOpenDebts = { navController.navigate(Routes.DEBTS) },
            )
        }

        composable(Routes.FORECAST) {
            CashFlowScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SPENDABLE) {
            SpendableScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.BURN_RATE) {
            BurnRateScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.FINANCIAL_HEALTH) {
            FinancialHealthScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.GOAL_PROJECTION) {
            GoalProjectionScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
