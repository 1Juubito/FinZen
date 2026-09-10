package com.finzen.app.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.RecurringTransactionEntity
import com.finzen.app.data.repository.AccountRepository
import com.finzen.app.data.repository.CategoryRepository
import com.finzen.app.data.repository.CreditCardRepository
import com.finzen.app.data.repository.RecurringTransactionRepository
import com.finzen.app.notifications.TransactionPrefill
import com.finzen.app.notifications.TransactionPrefillBus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringItemUi(
    val entity: RecurringTransactionEntity,
    val categoryName: String?,
    val categoryColor: String?,
    val categoryIcon: String?,
    val paymentName: String?,
)

class RecurringViewModel(
    private val repository: RecurringTransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    creditCardRepository: CreditCardRepository,
) : ViewModel() {

    val items: StateFlow<List<RecurringItemUi>> = combine(
        repository.observeAll(),
        categoryRepository.observeAll(),
        accountRepository.observeActive(),
        creditCardRepository.observeAll(),
    ) { recurrences, categories, accounts, cards ->
        recurrences.map { e ->
            val category = categories.firstOrNull { it.id == e.categoryId }
            RecurringItemUi(
                entity = e,
                categoryName = category?.name,
                categoryColor = category?.colorHex,
                categoryIcon = category?.iconKey,
                paymentName = cards.firstOrNull { it.id == e.creditCardId }?.name
                    ?: accounts.firstOrNull { it.id == e.accountId }?.name,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun postNow(item: RecurringTransactionEntity) {
        TransactionPrefillBus.submit(
            TransactionPrefill(
                type = item.type,
                amountCents = Math.round(item.amount * 100.0),
                description = item.description,
                categoryId = item.categoryId,
                accountId = item.accountId,
                creditCardId = item.creditCardId,
                recurringId = item.id,
            )
        )
    }

    fun skip(item: RecurringTransactionEntity) {
        viewModelScope.launch { repository.advance(item.id) }
    }

    fun setActive(item: RecurringTransactionEntity, active: Boolean) {
        viewModelScope.launch { repository.setActive(item.id, active) }
    }

    fun delete(item: RecurringTransactionEntity) {
        viewModelScope.launch { repository.delete(item) }
    }
}
