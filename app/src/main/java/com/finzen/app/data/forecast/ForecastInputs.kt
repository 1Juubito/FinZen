package com.finzen.app.data.forecast

import com.finzen.app.data.local.dao.CardInvoiceTotal
import com.finzen.app.data.local.dao.TransactionDetails
import com.finzen.app.data.local.entity.RecurringTransactionEntity
import com.finzen.app.data.model.TransactionType
import com.finzen.app.util.DateUtils

object ForecastInputs {

    fun recurring(items: List<RecurringTransactionEntity>): List<ForecastRecurring> =
        items.filter { it.type != TransactionType.TRANSFER }
            .map { r ->
                ForecastRecurring(
                    amount = r.amount,
                    isIncome = r.type == TransactionType.INCOME,
                    frequency = r.frequency,
                    interval = r.interval,
                    nextDueDate = DateUtils.toLocalDate(r.nextDueDate),
                    endDate = r.endDate?.let { DateUtils.toLocalDate(it) },
                )
            }

    fun pending(items: List<TransactionDetails>): List<ForecastFlow> =
        items.filter { it.transaction.type != TransactionType.TRANSFER }
            .map { d ->
                ForecastFlow(
                    amount = d.transaction.amount,
                    isIncome = d.transaction.type == TransactionType.INCOME,
                    date = DateUtils.toLocalDate(d.transaction.date),
                )
            }

    fun cardInvoices(items: List<CardInvoiceTotal>): List<ForecastFlow> =
        items.map { t ->
            ForecastFlow(amount = t.total, isIncome = false, date = DateUtils.toLocalDate(t.invoiceDate))
        }
}
