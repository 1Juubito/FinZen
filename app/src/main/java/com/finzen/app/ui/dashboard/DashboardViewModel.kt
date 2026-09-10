package com.finzen.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.insight.DashboardInsight
import com.finzen.app.data.insight.InsightsCalculator
import com.finzen.app.data.local.dao.CategorySpending
import com.finzen.app.data.local.dao.TransactionDetails
import com.finzen.app.data.model.AccountWithBalance
import com.finzen.app.data.model.DebtDirection
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.repository.AccountRepository
import com.finzen.app.data.repository.CreditCardRepository
import com.finzen.app.data.repository.PersonalDebtRepository
import com.finzen.app.data.repository.SettingsRepository
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.util.MonthRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategorySliceUi(
    val name: String,
    val colorHex: String,
    val total: Double,
    val fraction: Float,
)

data class ProjectionUi(
    val projectedBalance: Double,
    val expectedIncome: Double,
    val expectedExpense: Double,
)

data class DashboardCardInvoice(
    val name: String,
    val colorHex: String,
    val amount: Double,
)

data class DashboardUiState(
    val monthRef: MonthRef = MonthRef.now(),
    val totalBalance: Double = 0.0,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val expenseSlices: List<CategorySliceUi> = emptyList(),
    val totalExpenseForChart: Double = 0.0,
    val recent: List<TransactionDetails> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val balanceVisible: Boolean = true,
    val userName: String = "",
    val projection: ProjectionUi? = null,
    val insights: List<DashboardInsight> = emptyList(),
    val debtsTheyOwe: Double = 0.0,
    val debtsIOwe: Double = 0.0,
    val openInvoices: List<DashboardCardInvoice> = emptyList(),
    val loading: Boolean = true,
) {
    val monthResult: Double get() = income - expense
    val hasDebts: Boolean get() = debtsTheyOwe > 0.0 || debtsIOwe > 0.0
    val hasOpenInvoices: Boolean get() = openInvoices.isNotEmpty()
    val openInvoicesTotal: Double get() = openInvoices.sumOf { it.amount }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    personalDebtRepository: PersonalDebtRepository,
    creditCardRepository: CreditCardRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val month = MutableStateFlow(MonthRef.now())

    private val monthAggregate = month.flatMapLatest { ref ->
        val (start, end) = ref.bounds
        val (prevStart, prevEnd) = ref.previous().bounds
        combine(
            transactionRepository.observeTotalByType(TransactionType.INCOME, start, end),
            transactionRepository.observeTotalByType(TransactionType.EXPENSE, start, end),
            transactionRepository.observeSpendingByCategory(TransactionType.EXPENSE, start, end),
            transactionRepository.observeDetailsBetween(start, end),

            combine(
                transactionRepository.observeTotalByType(TransactionType.EXPENSE, prevStart, prevEnd),
                transactionRepository.observePendingByInvoiceMonth(TransactionType.INCOME, start, end),
                transactionRepository.observePendingByInvoiceMonth(TransactionType.EXPENSE, start, end),
            ) { prevExpense, pendingIncome, pendingExpense ->
                Triple(prevExpense, pendingIncome, pendingExpense)
            },
        ) { income, expense, spending, details, extra ->
            MonthAggregate(income, expense, spending, details, extra.first, extra.second, extra.third)
        }
    }

    private val settingsFlow = combine(
        settingsRepository.balanceVisible,
        settingsRepository.userName,
    ) { balanceVisible, userName -> balanceVisible to userName }

    private val debtsSummary = personalDebtRepository.observeAll().map { all ->
        val open = all.filterNot { it.settled }
        DebtsSummary(
            theyOwe = open.filter { it.direction == DebtDirection.THEY_OWE_ME }.sumOf { it.amount },
            iOwe = open.filter { it.direction == DebtDirection.I_OWE }.sumOf { it.amount },
        )
    }

    private val openInvoices = creditCardRepository.observeCardsWithUsage().map { cards ->
        cards.filter { it.currentInvoice > 0.0 }
            .map { DashboardCardInvoice(it.card.name, it.card.colorHex, it.currentInvoice) }
    }

    private val baseFlow = combine(
        accountRepository.observeAccountsWithBalance(),
        accountRepository.observeTotalBalance(),
        settingsFlow,
        debtsSummary,
        openInvoices,
    ) { accounts, totalBalance, settings, debts, invoices ->
        DashboardBase(accounts, totalBalance, settings.first, settings.second, debts, invoices)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        month,
        monthAggregate,
        baseFlow,
    ) { ref, aggregate, base ->

        val separateAccountIds = base.accounts
            .filterNot { it.account.includeInTotal }
            .map { it.account.id }
            .toSet()
        DashboardUiState(
            monthRef = ref,
            totalBalance = base.totalBalance,
            income = aggregate.income,
            expense = aggregate.expense,
            expenseSlices = buildSlices(aggregate.spending),
            totalExpenseForChart = aggregate.spending.sumOf { it.total },
            recent = aggregate.details
                .filterNot { it.transaction.accountId in separateAccountIds }
                .take(6),

            accounts = base.accounts,
            balanceVisible = base.balanceVisible,
            userName = base.userName,
            projection = buildProjection(
                ref = ref,
                totalBalance = base.totalBalance,
                pendingIncome = aggregate.pendingIncome,
                pendingExpense = aggregate.pendingExpense,
            ),
            insights = InsightsCalculator.build(
                income = aggregate.income,
                expense = aggregate.expense,
                previousExpense = aggregate.prevExpense,
                topCategoryName = aggregate.spending.firstOrNull()?.categoryName,
                topCategoryAmount = aggregate.spending.firstOrNull()?.total ?: 0.0,
            ),
            debtsTheyOwe = base.debts.theyOwe,
            debtsIOwe = base.debts.iOwe,
            openInvoices = base.openInvoices,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun previousMonth() { month.value = month.value.previous() }
    fun nextMonth() { month.value = month.value.next() }

    fun toggleBalanceVisibility() {
        viewModelScope.launch {
            settingsRepository.setBalanceVisible(!uiState.value.balanceVisible)
        }
    }

    private fun buildProjection(
        ref: MonthRef,
        totalBalance: Double,
        pendingIncome: Double,
        pendingExpense: Double,
    ): ProjectionUi? {
        if (ref != MonthRef.now()) return null
        if (pendingIncome <= 0.0 && pendingExpense <= 0.0) return null
        return ProjectionUi(
            projectedBalance = totalBalance + pendingIncome - pendingExpense,
            expectedIncome = pendingIncome,
            expectedExpense = pendingExpense,
        )
    }

    private fun buildSlices(spending: List<CategorySpending>): List<CategorySliceUi> {
        val total = spending.sumOf { it.total }
        if (total <= 0.0) return emptyList()
        val top = spending.take(5)
        val restTotal = spending.drop(5).sumOf { it.total }
        val slices = top.map {
            CategorySliceUi(it.categoryName, it.categoryColor, it.total, (it.total / total).toFloat())
        }.toMutableList()
        if (restTotal > 0.0) {
            slices += CategorySliceUi("Outros", "#868E96", restTotal, (restTotal / total).toFloat())
        }
        return slices
    }

    private data class MonthAggregate(
        val income: Double,
        val expense: Double,
        val spending: List<CategorySpending>,
        val details: List<TransactionDetails>,
        val prevExpense: Double,
        val pendingIncome: Double,
        val pendingExpense: Double,
    )

    private data class DashboardBase(
        val accounts: List<AccountWithBalance>,
        val totalBalance: Double,
        val balanceVisible: Boolean,
        val userName: String,
        val debts: DebtsSummary,
        val openInvoices: List<DashboardCardInvoice>,
    )

    private data class DebtsSummary(val theyOwe: Double, val iOwe: Double)
}
