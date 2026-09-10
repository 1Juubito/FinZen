package com.finzen.app.di

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.finzen.app.FinanceApp
import com.finzen.app.ui.accounts.AccountEditViewModel
import com.finzen.app.ui.accounts.AccountsViewModel
import com.finzen.app.ui.agenda.AgendaViewModel
import com.finzen.app.ui.budgets.BudgetsViewModel
import com.finzen.app.ui.burnrate.BurnRateViewModel
import com.finzen.app.ui.cards.CardDetailViewModel
import com.finzen.app.ui.cards.CardEditViewModel
import com.finzen.app.ui.cards.CardsViewModel
import com.finzen.app.ui.categories.CategoriesViewModel
import com.finzen.app.ui.categories.CategoryEditViewModel
import com.finzen.app.ui.dashboard.DashboardViewModel
import com.finzen.app.ui.debts.DebtPayoffViewModel
import com.finzen.app.ui.debts.DebtsViewModel
import com.finzen.app.ui.debts.ReceivablesViewModel
import com.finzen.app.ui.forecast.CashFlowViewModel
import com.finzen.app.ui.goals.GoalEditViewModel
import com.finzen.app.ui.goals.GoalProjectionViewModel
import com.finzen.app.ui.goals.GoalsViewModel
import com.finzen.app.ui.health.FinancialHealthViewModel
import com.finzen.app.ui.installments.InstallmentsViewModel
import com.finzen.app.ui.investments.InvestmentEditViewModel
import com.finzen.app.ui.investments.InvestmentsViewModel
import com.finzen.app.ui.reports.ReportsViewModel
import com.finzen.app.ui.recurring.RecurringViewModel
import com.finzen.app.ui.rules.CategoryRulesViewModel
import com.finzen.app.ui.settings.SettingsViewModel
import com.finzen.app.ui.spendable.SpendableViewModel
import com.finzen.app.ui.transaction.AddEditTransactionViewModel
import com.finzen.app.ui.transactions.TransactionsViewModel

object AppViewModelProvider {

    val Factory = viewModelFactory {

        initializer {
            val c = container()
            DashboardViewModel(
                c.accountRepository,
                c.transactionRepository,
                c.personalDebtRepository,
                c.creditCardRepository,
                c.settingsRepository,
            )
        }

        initializer {
            val c = container()
            TransactionsViewModel(c.transactionRepository, c.categoryRepository, c.accountRepository)
        }

        initializer {
            val c = container()
            AddEditTransactionViewModel(
                savedStateHandle = createSavedStateHandle(),
                transactionRepository = c.transactionRepository,
                categoryRepository = c.categoryRepository,
                accountRepository = c.accountRepository,
                creditCardRepository = c.creditCardRepository,
                categoryRuleRepository = c.categoryRuleRepository,
                recurringTransactionRepository = c.recurringTransactionRepository,
            )
        }

        initializer { AccountsViewModel(container().accountRepository) }
        initializer { AccountEditViewModel(createSavedStateHandle(), container().accountRepository) }

        initializer { CardsViewModel(container().creditCardRepository) }
        initializer {
            val c = container()
            CardDetailViewModel(createSavedStateHandle(), c.creditCardRepository, c.accountRepository, c.transactionRepository)
        }
        initializer { CardEditViewModel(createSavedStateHandle(), container().creditCardRepository) }

        initializer { CategoriesViewModel(container().categoryRepository) }
        initializer { CategoryEditViewModel(createSavedStateHandle(), container().categoryRepository) }

        initializer {
            val c = container()
            BudgetsViewModel(c.budgetRepository, c.categoryRepository)
        }

        initializer { ReportsViewModel(container().transactionRepository) }

        initializer { InstallmentsViewModel(container().transactionRepository) }
        initializer { DebtsViewModel(container().personalDebtRepository) }
        initializer { DebtPayoffViewModel(container().personalDebtRepository) }
        initializer { ReceivablesViewModel(container().personalDebtRepository) }

        initializer {
            val c = container()
            AgendaViewModel(
                c.creditCardRepository,
                c.transactionRepository,
                c.recurringTransactionRepository,
                c.personalDebtRepository,
            )
        }

        initializer {
            val c = container()
            CashFlowViewModel(
                c.accountRepository,
                c.recurringTransactionRepository,
                c.transactionRepository,
            )
        }

        initializer {
            val c = container()
            SpendableViewModel(
                c.accountRepository,
                c.recurringTransactionRepository,
                c.transactionRepository,
            )
        }

        initializer { BurnRateViewModel(container().transactionRepository) }

        initializer {
            val c = container()
            FinancialHealthViewModel(c.transactionRepository, c.accountRepository, c.recurringTransactionRepository)
        }

        initializer { GoalsViewModel(container().goalRepository) }
        initializer { GoalProjectionViewModel(container().goalRepository) }
        initializer { GoalEditViewModel(createSavedStateHandle(), container().goalRepository) }

        initializer {
            val c = container()
            InvestmentsViewModel(c.investmentRepository, c.accountRepository, c.transactionRepository)
        }
        initializer {
            val c = container()
            InvestmentEditViewModel(createSavedStateHandle(), c.investmentRepository, c.accountRepository, c.transactionRepository)
        }

        initializer {
            val c = container()
            RecurringViewModel(c.recurringTransactionRepository, c.categoryRepository, c.accountRepository, c.creditCardRepository)
        }

        initializer {
            val c = container()
            CategoryRulesViewModel(c.categoryRuleRepository, c.categoryRepository)
        }

        initializer {
            val c = container()
            SettingsViewModel(c.settingsRepository, c.database, c.backupManager)
        }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as FinanceApp).container
