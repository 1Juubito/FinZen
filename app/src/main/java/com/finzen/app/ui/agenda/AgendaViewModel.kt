package com.finzen.app.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.card.CardCycleCalculator
import com.finzen.app.data.model.DebtDirection
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.repository.CreditCardRepository
import com.finzen.app.data.repository.PersonalDebtRepository
import com.finzen.app.data.repository.RecurringTransactionRepository
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class AgendaKind { CARD_INVOICE, PENDING, RECURRING, DEBT }

data class AgendaItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val dueDate: Long,
    val isInflow: Boolean,
    val kind: AgendaKind,
    val colorHex: String?,
    val overdue: Boolean,
    val transactionId: Long? = null,
    val cardId: Long? = null,
)

data class AgendaGroup(
    val label: String,
    val items: List<AgendaItem>,
    val overdue: Boolean,
)

data class AgendaUiState(
    val groups: List<AgendaGroup> = emptyList(),
    val totalToPay: Double = 0.0,
    val totalToReceive: Double = 0.0,
    val overdueCount: Int = 0,
    val loading: Boolean = true,
) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

class AgendaViewModel(
    creditCardRepository: CreditCardRepository,
    transactionRepository: TransactionRepository,
    recurringTransactionRepository: RecurringTransactionRepository,
    personalDebtRepository: PersonalDebtRepository,
) : ViewModel() {

    val uiState: StateFlow<AgendaUiState> = combine(
        creditCardRepository.observeCardsWithUsage(),
        transactionRepository.observePendingDetails(),
        recurringTransactionRepository.observeActiveForTotals(),
        personalDebtRepository.observeAll(),
    ) { cards, pending, recurring, debts ->
        val today = LocalDate.now()

        fun normalize(millis: Long): Pair<Long, Boolean> {
            val ld = DateUtils.toLocalDate(millis)
            return DateUtils.localDateToMillis(ld) to ld.isBefore(today)
        }

        val items = buildList {

            cards.forEach { c ->
                if (c.currentInvoice <= 0.0) return@forEach
                val cycle = CardCycleCalculator.currentCycle(today, c.card.closingDay, c.card.dueDay)
                val (due, overdue) = normalize(DateUtils.localDateToMillis(cycle.dueDate))
                add(
                    AgendaItem(
                        key = "card-${c.card.id}",
                        title = c.card.name,
                        subtitle = "Fatura do cartão",
                        amount = c.currentInvoice,
                        dueDate = due,
                        isInflow = false,
                        kind = AgendaKind.CARD_INVOICE,
                        colorHex = c.card.colorHex,
                        overdue = overdue,
                        cardId = c.card.id,
                    ),
                )
            }

            pending.forEach { d ->
                val t = d.transaction
                val inflow = t.type == TransactionType.INCOME
                val (due, overdue) = normalize(t.date)
                add(
                    AgendaItem(
                        key = "tx-${t.id}",
                        title = t.description,
                        subtitle = d.categoryName ?: d.accountName ?: if (inflow) "A receber" else "A pagar",
                        amount = t.amount,
                        dueDate = due,
                        isInflow = inflow,
                        kind = AgendaKind.PENDING,
                        colorHex = d.categoryColor,
                        overdue = overdue,
                        transactionId = t.id,
                    ),
                )
            }

            recurring.forEach { r ->
                val inflow = r.type == TransactionType.INCOME
                val (due, overdue) = normalize(r.nextDueDate)
                add(
                    AgendaItem(
                        key = "rec-${r.id}",
                        title = r.description,
                        subtitle = "Recorrente",
                        amount = r.amount,
                        dueDate = due,
                        isInflow = inflow,
                        kind = AgendaKind.RECURRING,
                        colorHex = null,
                        overdue = overdue,
                    ),
                )
            }

            debts.filter { !it.settled && it.dueDate != null }.forEach { debt ->
                val inflow = debt.direction == DebtDirection.THEY_OWE_ME
                val (due, overdue) = normalize(debt.dueDate!!)
                add(
                    AgendaItem(
                        key = "debt-${debt.id}",
                        title = debt.person,
                        subtitle = if (inflow) "Vão te pagar" else "Você deve",
                        amount = debt.amount,
                        dueDate = due,
                        isInflow = inflow,
                        kind = AgendaKind.DEBT,
                        colorHex = null,
                        overdue = overdue,
                    ),
                )
            }
        }

        val (overdueItems, upcoming) = items.partition { it.overdue }
        val groups = buildList {
            if (overdueItems.isNotEmpty()) {
                add(
                    AgendaGroup(
                        label = "Atrasados",
                        items = overdueItems.sortedBy { it.dueDate },
                        overdue = true,
                    ),
                )
            }
            upcoming
                .groupBy { it.dueDate }
                .toSortedMap()
                .forEach { (day, dayItems) ->
                    add(
                        AgendaGroup(
                            label = DateUtils.dayHeaderLabel(day),
                            items = dayItems.sortedByDescending { it.amount },
                            overdue = false,
                        ),
                    )
                }
        }

        AgendaUiState(
            groups = groups,
            totalToPay = items.filterNot { it.isInflow }.sumOf { it.amount },
            totalToReceive = items.filter { it.isInflow }.sumOf { it.amount },
            overdueCount = overdueItems.size,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaUiState())
}
