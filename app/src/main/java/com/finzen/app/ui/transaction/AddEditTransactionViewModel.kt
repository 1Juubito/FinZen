package com.finzen.app.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.card.CardCycleCalculator
import com.finzen.app.data.category.CategoryRuleMatcher
import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.local.entity.CreditCardEntity
import com.finzen.app.data.local.entity.RecurringTransactionEntity
import com.finzen.app.data.local.entity.TransactionEntity
import com.finzen.app.data.model.CategoryType
import com.finzen.app.data.model.RecurrenceFrequency
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.recurrence.RecurrenceScheduler
import com.finzen.app.data.repository.AccountRepository
import com.finzen.app.data.repository.CategoryRepository
import com.finzen.app.data.repository.CategoryRuleRepository
import com.finzen.app.data.repository.CreditCardRepository
import com.finzen.app.data.repository.RecurringTransactionRepository
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.notifications.TransactionPrefillBus
import com.finzen.app.ui.navigation.Routes
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditTransactionUiState(
    val isEditing: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountDigits: String = "",
    val description: String = "",
    val date: Long = DateUtils.now(),
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val creditCardId: Long? = null,
    val isPaid: Boolean = true,
    val notes: String = "",
    val installments: Int = 1,
    val repeat: RecurrenceFrequency? = null,
    val repeatCount: Int? = null,
    val allCategories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val cards: List<CreditCardEntity> = emptyList(),
    val saved: Boolean = false,
) {
    val amount: Double get() = Money.centsToValue(amountDigits)
    val amountFormatted: String get() = Money.formatCentsInput(amountDigits)

    val categories: List<CategoryEntity>
        get() = when (type) {
            TransactionType.INCOME -> allCategories.filter { it.type == CategoryType.INCOME }
            else -> allCategories.filter { it.type == CategoryType.EXPENSE }
        }

    val selectedCategory: CategoryEntity? get() = allCategories.firstOrNull { it.id == categoryId }
    val selectedAccount: AccountEntity? get() = accounts.firstOrNull { it.id == accountId }
    val selectedToAccount: AccountEntity? get() = accounts.firstOrNull { it.id == toAccountId }
    val selectedCard: CreditCardEntity? get() = cards.firstOrNull { it.id == creditCardId }

    val showInstallments: Boolean get() = type == TransactionType.EXPENSE && creditCardId != null

    val canRepeat: Boolean get() = type != TransactionType.TRANSFER
    val repeatLabel: String
        get() = repeat?.let { if (repeatCount == null) it.label else "${it.label} · ${repeatCount}x" }
            ?: "Não repetir"

    val showRepeatCount: Boolean get() = repeat != null
    val repeatCountLabel: String get() = repeatCount?.let { "$it vezes" } ?: "Para sempre"

    val canSave: Boolean
        get() = amount > 0.0 && when (type) {
            TransactionType.TRANSFER ->
                accountId != null && toAccountId != null && accountId != toAccountId
            else ->
                categoryId != null && (accountId != null || creditCardId != null)
        }
}

class AddEditTransactionViewModel(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    creditCardRepository: CreditCardRepository,
    categoryRuleRepository: CategoryRuleRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
) : ViewModel() {

    private val txId: Long = savedStateHandle.get<Long>(Routes.ARG_TX_ID) ?: -1L
    private val initialType: TransactionType = runCatching {
        TransactionType.valueOf(savedStateHandle.get<String>(Routes.ARG_TX_TYPE) ?: TransactionType.EXPENSE.name)
    }.getOrDefault(TransactionType.EXPENSE)
    private val presetCardId: Long = savedStateHandle.get<Long>(Routes.ARG_CARD_ID) ?: -1L

    private var userPickedCategory = false

    private var pendingRecurringId: Long? = null

    private var rules: List<CategoryRuleMatcher.Rule> = emptyList()

    private val _state = MutableStateFlow(
        AddEditTransactionUiState(
            isEditing = txId > 0L,
            type = initialType,
            creditCardId = presetCardId.takeIf { it > 0L },
            isPaid = !(initialType == TransactionType.EXPENSE && presetCardId > 0L),
        )
    )
    val state = _state.asStateFlow()

    init {

        if (txId <= 0L) {
            TransactionPrefillBus.consume()?.let { prefill ->
                pendingRecurringId = prefill.recurringId
                if (prefill.categoryId != null) userPickedCategory = true
                _state.update {
                    it.copy(
                        type = prefill.type,
                        amountDigits = prefill.amountCents.toString().take(12),
                        description = prefill.description,
                        categoryId = prefill.categoryId ?: it.categoryId,
                        accountId = prefill.accountId ?: it.accountId,
                        creditCardId = prefill.creditCardId ?: it.creditCardId,
                        isPaid = if (prefill.creditCardId != null) false else it.isPaid,
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(
                categoryRepository.observeAll(),
                accountRepository.observeActive(),
                creditCardRepository.observeAll(),
            ) { categories, accounts, cards -> Triple(categories, accounts, cards) }
                .collect { (categories, accounts, cards) ->
                    _state.update { current ->
                        val defaultAccount = when {
                            current.accountId != null -> current.accountId
                            current.creditCardId != null -> null
                            else -> accounts.firstOrNull()?.id
                        }
                        applyAutoCategory(
                            current.copy(
                                allCategories = categories,
                                accounts = accounts,
                                cards = cards,
                                accountId = defaultAccount,
                            )
                        )
                    }
                }
        }
        viewModelScope.launch {
            categoryRuleRepository.observeAll().collect { list ->
                rules = list.map { CategoryRuleMatcher.Rule(it.keyword, it.categoryId) }
                _state.update { applyAutoCategory(it) }
            }
        }
        if (txId > 0L) loadExisting()
    }

    private fun applyAutoCategory(s: AddEditTransactionUiState): AddEditTransactionUiState {
        if (userPickedCategory || s.type == TransactionType.TRANSFER) return s
        val matchedId = CategoryRuleMatcher.match(s.description, rules)
            ?: return if (s.categoryId != null) s.copy(categoryId = null) else s
        val wantedType = if (s.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        val category = s.allCategories.firstOrNull { it.id == matchedId && it.type == wantedType } ?: return s
        return s.copy(categoryId = category.id)
    }

    private fun loadExisting() {
        viewModelScope.launch {
            val tx = transactionRepository.getById(txId) ?: return@launch
            userPickedCategory = true
            _state.update {
                it.copy(
                    isEditing = true,
                    type = tx.type,
                    amountDigits = toDigits(tx.amount),
                    description = tx.description,
                    date = tx.date,
                    categoryId = tx.categoryId,
                    accountId = tx.accountId,
                    toAccountId = tx.toAccountId,
                    creditCardId = tx.creditCardId,
                    isPaid = tx.isPaid,
                    notes = tx.notes.orEmpty(),
                    installments = tx.installmentTotal ?: 1,
                )
            }
        }
    }

    fun setType(type: TransactionType) {
        userPickedCategory = false
        _state.update {
            applyAutoCategory(
                it.copy(
                    type = type,
                    categoryId = null,
                    repeat = if (type == TransactionType.TRANSFER) null else it.repeat,
                    creditCardId = if (type == TransactionType.TRANSFER) null else it.creditCardId,
                    toAccountId = if (type == TransactionType.TRANSFER) it.toAccountId else null,
                )
            )
        }
    }

    fun appendDigit(digit: Char) {
        if (!digit.isDigit()) return
        _state.update {
            val next = (it.amountDigits + digit).trimStart('0').take(12)
            it.copy(amountDigits = next)
        }
    }

    fun backspace() {
        _state.update { it.copy(amountDigits = it.amountDigits.dropLast(1)) }
    }

    fun setDescription(value: String) = _state.update { applyAutoCategory(it.copy(description = value)) }
    fun setDate(value: Long) = _state.update { it.copy(date = value) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }
    fun setRepeat(frequency: RecurrenceFrequency?) = _state.update {
        it.copy(repeat = frequency, repeatCount = if (frequency == null) null else it.repeatCount)
    }

    fun setRepeatCount(count: Int?) = _state.update { it.copy(repeatCount = count) }

    fun selectCategory(id: Long) {
        userPickedCategory = true
        _state.update { it.copy(categoryId = id) }
    }

    fun togglePaid() = _state.update { it.copy(isPaid = !it.isPaid) }
    fun setInstallments(count: Int) = _state.update { it.copy(installments = count.coerceIn(1, 48)) }

    fun selectAccount(id: Long) = _state.update {
        it.copy(accountId = id, creditCardId = null)
    }

    fun selectToAccount(id: Long) = _state.update { it.copy(toAccountId = id) }

    fun selectCard(id: Long) = _state.update {
        it.copy(creditCardId = id, accountId = null, isPaid = false)
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val entity = TransactionEntity(
                id = if (s.isEditing) txId else 0L,
                description = s.description.ifBlank {
                    s.selectedCategory?.name ?: s.type.label
                },
                amount = s.amount,
                type = s.type,
                date = s.date,
                invoiceDate = invoiceDateFor(s),
                categoryId = if (s.type == TransactionType.TRANSFER) null else s.categoryId,
                accountId = s.accountId,
                toAccountId = if (s.type == TransactionType.TRANSFER) s.toAccountId else null,
                creditCardId = if (s.type == TransactionType.TRANSFER) null else s.creditCardId,
                isPaid = s.isPaid,
                notes = s.notes.ifBlank { null },
            )
            when {
                s.isEditing -> transactionRepository.update(entity)
                s.showInstallments && s.installments > 1 ->
                    transactionRepository.saveInstallments(
                        entity, s.installments, s.selectedCard?.closingDay, s.selectedCard?.dueDay,
                    )
                else -> transactionRepository.insert(entity)
            }
            if (!s.isEditing) {
                val recurringId = pendingRecurringId
                when {
                    recurringId != null -> recurringTransactionRepository.advance(recurringId)
                    s.repeat != null && s.type != TransactionType.TRANSFER -> createRecurrence(s, entity)
                }
            }
            _state.update { it.copy(saved = true) }
        }
    }

    private fun invoiceDateFor(s: AddEditTransactionUiState): Long {
        val card = s.selectedCard
        return if (s.type == TransactionType.EXPENSE && card != null) {
            val cycle = CardCycleCalculator.currentCycle(DateUtils.toLocalDate(s.date), card.closingDay, card.dueDay)
            DateUtils.localDateToMillis(cycle.closingDate)
        } else {
            s.date
        }
    }

    private suspend fun createRecurrence(s: AddEditTransactionUiState, base: TransactionEntity) {
        val frequency = s.repeat ?: return
        val count = s.repeatCount
        if (count != null && count <= 1) return
        val startDate = DateUtils.toLocalDate(s.date)
        val next = RecurrenceScheduler.advance(startDate, frequency)

        val endDate = count?.let { n ->
            var date = startDate
            repeat(n - 1) { date = RecurrenceScheduler.advance(date, frequency) }
            DateUtils.localDateToMillis(date)
        }
        recurringTransactionRepository.upsert(
            RecurringTransactionEntity(
                description = base.description,
                amount = base.amount,
                type = base.type,
                categoryId = base.categoryId,
                accountId = base.accountId,
                creditCardId = base.creditCardId,
                notes = base.notes,
                frequency = frequency,
                interval = 1,
                startDate = s.date,
                endDate = endDate,
                nextDueDate = DateUtils.localDateToMillis(next),
                active = true,
            )
        )
    }

    fun delete() {
        if (txId <= 0L) return
        viewModelScope.launch {
            transactionRepository.getById(txId)?.let { transactionRepository.delete(it) }
            _state.update { it.copy(saved = true) }
        }
    }

    private fun toDigits(amount: Double): String {
        val cents = Math.round(amount * 100.0)
        return if (cents <= 0) "" else cents.toString()
    }
}
