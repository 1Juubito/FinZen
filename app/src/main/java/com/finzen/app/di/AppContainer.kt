package com.finzen.app.di

import android.content.Context
import com.finzen.app.data.backup.BackupManager
import com.finzen.app.data.local.FinanceDatabase
import com.finzen.app.data.repository.AccountRepository
import com.finzen.app.data.repository.BudgetRepository
import com.finzen.app.data.repository.CategoryRepository
import com.finzen.app.data.repository.CategoryRuleRepository
import com.finzen.app.data.repository.CreditCardRepository
import com.finzen.app.data.repository.GoalRepository
import com.finzen.app.data.repository.InvestmentRepository
import com.finzen.app.data.repository.PersonalDebtRepository
import com.finzen.app.data.repository.RecurringTransactionRepository
import com.finzen.app.data.repository.SettingsRepository
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.notifications.BudgetReminder
import com.finzen.app.notifications.InvoiceReminder
import com.finzen.app.notifications.RecurringReminder

interface AppContainer {
    val database: FinanceDatabase
    val accountRepository: AccountRepository
    val categoryRepository: CategoryRepository
    val transactionRepository: TransactionRepository
    val creditCardRepository: CreditCardRepository
    val budgetRepository: BudgetRepository
    val goalRepository: GoalRepository
    val investmentRepository: InvestmentRepository
    val recurringTransactionRepository: RecurringTransactionRepository
    val categoryRuleRepository: CategoryRuleRepository
    val personalDebtRepository: PersonalDebtRepository
    val settingsRepository: SettingsRepository
    val backupManager: BackupManager
    val recurringReminder: RecurringReminder
    val invoiceReminder: InvoiceReminder
    val budgetReminder: BudgetReminder
}

class AppDataContainer(private val context: Context) : AppContainer {

    override val database: FinanceDatabase by lazy { FinanceDatabase.get(context) }

    override val accountRepository: AccountRepository by lazy {
        AccountRepository(database.accountDao(), database.transactionDao())
    }
    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao())
    }
    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }
    override val creditCardRepository: CreditCardRepository by lazy {
        CreditCardRepository(database.creditCardDao(), database.transactionDao())
    }
    override val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(database.budgetDao(), database.transactionDao(), database.categoryDao())
    }
    override val goalRepository: GoalRepository by lazy {
        GoalRepository(database.goalDao())
    }
    override val investmentRepository: InvestmentRepository by lazy {
        InvestmentRepository(database.investmentDao())
    }
    override val recurringTransactionRepository: RecurringTransactionRepository by lazy {
        RecurringTransactionRepository(database.recurringTransactionDao())
    }
    override val categoryRuleRepository: CategoryRuleRepository by lazy {
        CategoryRuleRepository(database.categoryRuleDao())
    }
    override val personalDebtRepository: PersonalDebtRepository by lazy {
        PersonalDebtRepository(database.personalDebtDao())
    }
    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }
    override val backupManager: BackupManager by lazy {
        BackupManager(context, database, settingsRepository)
    }
    override val recurringReminder: RecurringReminder by lazy {
        RecurringReminder(context, recurringTransactionRepository)
    }
    override val invoiceReminder: InvoiceReminder by lazy {
        InvoiceReminder(context, creditCardRepository, transactionRepository)
    }
    override val budgetReminder: BudgetReminder by lazy {
        BudgetReminder(context, budgetRepository, settingsRepository)
    }
}
