package com.finzen.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.dao.TransactionDetails
import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.repository.AccountRepository
import com.finzen.app.data.repository.CategoryRepository
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.util.DateUtils
import com.finzen.app.util.MonthRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayGroup(
    val dateMillis: Long,
    val label: String,
    val items: List<TransactionDetails>,
    val dayResult: Double,
)

data class TransactionsUiState(
    val monthRef: MonthRef = MonthRef.now(),
    val query: String = "",
    val filter: TransactionType? = null,
    val categoryId: Long? = null,
    val pendingOnly: Boolean = false,
    val accountId: Long? = null,
    val searching: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val groups: List<DayGroup> = emptyList(),
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val accumulatedBalance: Double = 0.0,
    val resultCount: Int = 0,
    val isEmpty: Boolean = true,
) {
    val balance: Double get() = income - expense
    val selectedCategory: CategoryEntity? get() = categories.firstOrNull { it.id == categoryId }
    val selectedAccount: AccountEntity? get() = accounts.firstOrNull { it.id == accountId }
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || filter != null || categoryId != null || pendingOnly || accountId != null
}

private data class Filters(
    val query: String,
    val type: TransactionType?,
    val categoryId: Long?,
    val pendingOnly: Boolean,
    val accountId: Long?,
) {

    val searching: Boolean get() = query.isNotBlank()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val month = MutableStateFlow(MonthRef.now())
    private val query = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<TransactionType?>(null)
    private val categoryFilter = MutableStateFlow<Long?>(null)
    private val pendingOnly = MutableStateFlow(false)
    private val accountFilter = MutableStateFlow<Long?>(null)

    private val filters =
        combine(query, typeFilter, categoryFilter, pendingOnly, accountFilter) { q, type, category, pending, account ->
            Filters(q.trim(), type, category, pending, account)
        }

    private val searchingMode = filters.map { it.searching }.distinctUntilChanged()
    private val sourceDetails = combine(month, searchingMode) { ref, searching -> ref to searching }
        .flatMapLatest { (ref, searching) ->
            if (searching) {
                transactionRepository.observeAllDetails()
            } else {
                val (start, end) = ref.bounds
                transactionRepository.observeDetailsBetween(start, end)
            }
        }

    private val runningBalance = combine(month, accountFilter) { ref, accId -> ref to accId }
        .flatMapLatest { (ref, accId) ->
            if (accId != null) {
                transactionRepository.observeAccountBalanceAsOf(accId, ref.bounds.second)
            } else {
                transactionRepository.observeBalanceAsOf(ref.bounds.first)
            }
        }

    private val categoriesAndAccounts =
        combine(categoryRepository.observeAll(), accountRepository.observeActive()) { c, a -> c to a }

    val uiState: StateFlow<TransactionsUiState> = combine(
        month,
        filters,
        sourceDetails,
        categoriesAndAccounts,
        runningBalance,
    ) { ref, f, source, (categories, accounts), running ->

        val separateIds = accounts.filterNot { it.includeInTotal }.map { it.id }.toSet()
        val scoped = source.filter { d ->
            (f.categoryId == null || d.transaction.categoryId == f.categoryId) &&
                (f.query.isBlank() || d.matches(f.query)) &&
                (!f.pendingOnly || !d.transaction.isPaid) &&
                (
                    if (f.accountId != null) {
                        d.transaction.accountId == f.accountId
                    } else {
                        d.transaction.accountId !in separateIds
                    }
                )
        }
        val filtered = if (f.type == null) scoped else scoped.filter { it.transaction.type == f.type }

        val groups = filtered
            .groupBy { DateUtils.startOfDay(it.transaction.date) }
            .map { (day, items) ->
                DayGroup(
                    dateMillis = day,
                    label = DateUtils.dayHeaderLabel(day),
                    items = items,
                    dayResult = items.sumOf { it.signedAmount() },
                )
            }
            .sortedByDescending { it.dateMillis }

        val income = scoped.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
        val expense = scoped.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }

        TransactionsUiState(
            monthRef = ref,
            query = f.query,
            filter = f.type,
            categoryId = f.categoryId,
            pendingOnly = f.pendingOnly,
            accountId = f.accountId,
            searching = f.searching,
            categories = categories,
            accounts = accounts,
            groups = groups,
            income = income,
            expense = expense,

            accumulatedBalance = if (f.accountId != null) {
                (accounts.firstOrNull { it.id == f.accountId }?.initialBalance ?: 0.0) + running
            } else {
                running + income - expense
            },
            resultCount = filtered.size,
            isEmpty = filtered.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun previousMonth() { month.value = month.value.previous() }
    fun nextMonth() { month.value = month.value.next() }
    fun setFilter(type: TransactionType?) { typeFilter.value = type }
    fun setQuery(value: String) { query.value = value }
    fun setCategory(id: Long?) { categoryFilter.value = id }
    fun setPendingOnly(value: Boolean) { pendingOnly.value = value }
    fun setAccount(id: Long?) { accountFilter.value = id }

    fun markPaid(id: Long) {
        viewModelScope.launch { transactionRepository.setPaid(id, true) }
    }

    fun clearFilters() {
        query.value = ""
        typeFilter.value = null
        categoryFilter.value = null
        pendingOnly.value = false
        accountFilter.value = null
    }

    private fun TransactionDetails.matches(q: String): Boolean {
        val needle = q.lowercase()
        return transaction.description.lowercase().contains(needle) ||
            (categoryName?.lowercase()?.contains(needle) == true) ||
            (accountName?.lowercase()?.contains(needle) == true) ||
            (cardName?.lowercase()?.contains(needle) == true) ||
            (transaction.notes?.lowercase()?.contains(needle) == true)
    }

    private fun TransactionDetails.signedAmount(): Double = when (transaction.type) {
        TransactionType.INCOME -> transaction.amount
        TransactionType.EXPENSE -> -transaction.amount
        TransactionType.TRANSFER -> 0.0
    }
}
