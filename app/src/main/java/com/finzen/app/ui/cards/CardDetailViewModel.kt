package com.finzen.app.ui.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.card.CardCycle
import com.finzen.app.data.card.CardCycleCalculator
import com.finzen.app.data.local.dao.TransactionDetails
import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.local.entity.CreditCardEntity
import com.finzen.app.data.local.entity.TransactionEntity
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.repository.AccountRepository
import com.finzen.app.data.repository.CreditCardRepository
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.ui.navigation.Routes
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class InvoiceStatus { OPEN, CLOSED, PAID, EMPTY }

data class CardDetailUiState(
    val card: CreditCardEntity? = null,
    val cycle: CardCycle? = null,
    val items: List<TransactionDetails> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val invoiceTotal: Double = 0.0,
    val status: InvoiceStatus = InvoiceStatus.EMPTY,
) {
    val cycleLabel: String
        get() = cycle?.let { DateUtils.monthYearLabel(it.refYear, it.refMonth) } ?: ""
}

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailViewModel(
    savedStateHandle: SavedStateHandle,
    creditCardRepository: CreditCardRepository,
    accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    val cardId: Long = savedStateHandle.get<Long>(Routes.ARG_CARD_ID) ?: -1L

    private val offset = MutableStateFlow(0)
    private val cardFlow = creditCardRepository.observeById(cardId)

    private val cycleFlow = combine(cardFlow, offset) { card, off ->
        card?.let { CardCycleCalculator.cycleForOffset(LocalDate.now(), it.closingDay, it.dueDay, off) }
    }

    private val items = cycleFlow.flatMapLatest { cycle ->
        if (cycle == null) {
            flowOf(emptyList())
        } else {
            transactionRepository.observeCardDetails(
                cardId,
                DateUtils.localDateToMillis(cycle.periodStart),
                DateUtils.localDateToMillis(cycle.periodEndExclusive),
            )
        }
    }

    val uiState: StateFlow<CardDetailUiState> = combine(
        cardFlow,
        cycleFlow,
        items,
        accountRepository.observeActive(),
    ) { card, cycle, list, accounts ->
        val expenses = list.filter { it.transaction.type == TransactionType.EXPENSE }
        val status = when {
            expenses.isEmpty() -> InvoiceStatus.EMPTY
            expenses.all { it.transaction.isPaid } -> InvoiceStatus.PAID
            cycle != null && LocalDate.now().isAfter(cycle.closingDate) -> InvoiceStatus.CLOSED
            else -> InvoiceStatus.OPEN
        }
        CardDetailUiState(
            card = card,
            cycle = cycle,
            items = list,
            accounts = accounts,
            invoiceTotal = expenses.sumOf { it.transaction.amount },
            status = status,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardDetailUiState())

    fun previousCycle() { offset.value -= 1 }
    fun nextCycle() { offset.value += 1 }

    fun payInvoice(accountId: Long) {
        val state = uiState.value
        val cycle = state.cycle ?: return
        val start = DateUtils.localDateToMillis(cycle.periodStart)
        val end = DateUtils.localDateToMillis(cycle.periodEndExclusive)
        val total = state.invoiceTotal
        val cardName = state.card?.name ?: "cartão"
        viewModelScope.launch {
            transactionRepository.setCardInvoicePaid(cardId, start, end, true)
            if (total > 0.0) {
                transactionRepository.insert(
                    TransactionEntity(
                        description = "Pagamento da fatura • $cardName",
                        amount = total,
                        type = TransactionType.TRANSFER,
                        date = DateUtils.now(),
                        accountId = accountId,
                        toAccountId = null,
                        creditCardId = null,
                        isPaid = true,
                        groupId = invoiceGroupId(start),
                    )
                )
            }
        }
    }

    fun reopenInvoice() {
        val cycle = uiState.value.cycle ?: return
        val start = DateUtils.localDateToMillis(cycle.periodStart)
        val end = DateUtils.localDateToMillis(cycle.periodEndExclusive)
        viewModelScope.launch {
            transactionRepository.setCardInvoicePaid(cardId, start, end, false)
            transactionRepository.deleteGroup(invoiceGroupId(start))
        }
    }

    private fun invoiceGroupId(periodStart: Long): String = "invoice:$cardId:$periodStart"
}
