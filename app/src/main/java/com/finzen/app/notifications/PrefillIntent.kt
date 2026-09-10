package com.finzen.app.notifications

import android.content.Context
import android.content.Intent
import com.finzen.app.MainActivity
import com.finzen.app.data.model.TransactionType

object PrefillIntent {

    const val ACTION = "com.finzen.app.ADD_PREFILLED_TRANSACTION"

    private const val EXTRA_TYPE = "extra_type"
    private const val EXTRA_AMOUNT_CENTS = "extra_amount_cents"
    private const val EXTRA_DESCRIPTION = "extra_description"
    private const val EXTRA_CATEGORY_ID = "extra_category_id"
    private const val EXTRA_ACCOUNT_ID = "extra_account_id"
    private const val EXTRA_CARD_ID = "extra_card_id"
    private const val EXTRA_RECURRING_ID = "extra_recurring_id"

    private const val ABSENT = -1L

    fun build(context: Context, prefill: TransactionPrefill): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = ACTION
            putExtra(EXTRA_TYPE, prefill.type.name)
            putExtra(EXTRA_AMOUNT_CENTS, prefill.amountCents)
            putExtra(EXTRA_DESCRIPTION, prefill.description)
            putExtra(EXTRA_CATEGORY_ID, prefill.categoryId ?: ABSENT)
            putExtra(EXTRA_ACCOUNT_ID, prefill.accountId ?: ABSENT)
            putExtra(EXTRA_CARD_ID, prefill.creditCardId ?: ABSENT)
            putExtra(EXTRA_RECURRING_ID, prefill.recurringId ?: ABSENT)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    fun parse(intent: Intent?): TransactionPrefill? {
        if (intent?.action != ACTION) return null
        val cents = intent.getLongExtra(EXTRA_AMOUNT_CENTS, 0L)
        if (cents <= 0L) return null
        val type = runCatching {
            TransactionType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: TransactionType.EXPENSE.name)
        }.getOrDefault(TransactionType.EXPENSE)
        return TransactionPrefill(
            type = type,
            amountCents = cents,
            description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty(),
            categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, ABSENT).takeIf { it > 0L },
            accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, ABSENT).takeIf { it > 0L },
            creditCardId = intent.getLongExtra(EXTRA_CARD_ID, ABSENT).takeIf { it > 0L },
            recurringId = intent.getLongExtra(EXTRA_RECURRING_ID, ABSENT).takeIf { it > 0L },
        )
    }
}
