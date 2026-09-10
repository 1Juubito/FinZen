package com.finzen.app.notifications

import com.finzen.app.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TransactionPrefill(
    val type: TransactionType,
    val amountCents: Long,
    val description: String,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val creditCardId: Long? = null,
    val recurringId: Long? = null,
)

object TransactionPrefillBus {

    private val _pending = MutableStateFlow<TransactionPrefill?>(null)
    val pending: StateFlow<TransactionPrefill?> = _pending.asStateFlow()

    fun submit(prefill: TransactionPrefill) {
        _pending.value = prefill
    }

    fun consume(): TransactionPrefill? {
        val current = _pending.value
        _pending.value = null
        return current
    }
}
