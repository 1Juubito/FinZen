package com.finzen.app.data.repository

import com.finzen.app.data.card.CardCycleCalculator
import com.finzen.app.data.local.dao.AmountPoint
import com.finzen.app.data.local.dao.CardInvoiceTotal
import com.finzen.app.data.local.dao.CardSpent
import com.finzen.app.data.local.dao.CategorySpending
import com.finzen.app.data.local.dao.TransactionDao
import com.finzen.app.data.local.dao.TransactionDetails
import com.finzen.app.data.local.entity.CreditCardEntity
import com.finzen.app.data.local.entity.TransactionEntity
import com.finzen.app.data.model.TransactionType
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun observeDetailsBetween(start: Long, end: Long): Flow<List<TransactionDetails>> =
        transactionDao.observeDetailsBetween(start, end)

    fun observeRecent(limit: Int = 8): Flow<List<TransactionDetails>> =
        transactionDao.observeRecentDetails(limit)

    fun observeAllDetails(): Flow<List<TransactionDetails>> =
        transactionDao.observeAllDetails()

    fun observeFutureInstallments(from: Long): Flow<List<TransactionDetails>> =
        transactionDao.observeFutureInstallments(from)

    fun observePendingDetails(): Flow<List<TransactionDetails>> =
        transactionDao.observePendingDetails()

    fun observeCardDetails(cardId: Long, start: Long, end: Long): Flow<List<TransactionDetails>> =
        transactionDao.observeCardDetails(cardId, start, end)

    fun observeTotalByType(type: TransactionType, start: Long, end: Long): Flow<Double> =
        transactionDao.observeTotalByType(type, start, end)

    fun observePendingTotalByType(type: TransactionType, start: Long, end: Long): Flow<Double> =
        transactionDao.observePendingTotalByType(type, start, end)

    fun observePendingByInvoiceMonth(type: TransactionType, start: Long, end: Long): Flow<Double> =
        transactionDao.observePendingByInvoiceMonth(type, start, end)

    fun observeBalanceAsOf(asOf: Long): Flow<Double> =
        transactionDao.observeBalanceAsOf(asOf)

    fun observeAccountBalanceAsOf(accountId: Long, asOf: Long): Flow<Double> =
        transactionDao.observeAccountBalanceAsOf(accountId, asOf)

    fun observeSpendingByCategory(type: TransactionType, start: Long, end: Long): Flow<List<CategorySpending>> =
        transactionDao.observeSpendingByCategory(type, start, end)

    fun observeExpensePoints(start: Long, end: Long): Flow<List<AmountPoint>> =
        transactionDao.observeExpensePoints(start, end)

    fun observeOpenCardTotals(): Flow<List<CardSpent>> = transactionDao.observeOpenCardTotals()

    fun observeOpenCardInvoiceTotals(): Flow<List<CardInvoiceTotal>> =
        transactionDao.observeOpenCardInvoiceTotals()

    suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)
    suspend fun count(): Int = transactionDao.count()

    suspend fun insert(transaction: TransactionEntity): Long = transactionDao.insert(transaction)
    suspend fun update(transaction: TransactionEntity) = transactionDao.update(transaction)
    suspend fun delete(transaction: TransactionEntity) = transactionDao.delete(transaction)
    suspend fun deleteGroup(groupId: String) = transactionDao.deleteGroup(groupId)
    suspend fun setPaid(id: Long, paid: Boolean) = transactionDao.setPaid(id, paid)

    suspend fun setCardInvoicePaid(cardId: Long, start: Long, end: Long, paid: Boolean) =
        transactionDao.setCardInvoicePaid(cardId, start, end, paid)

    suspend fun unpaidCardTotal(cardId: Long, start: Long, end: Long): Double =
        transactionDao.unpaidCardTotal(cardId, start, end)

    suspend fun saveInstallments(
        base: TransactionEntity,
        installments: Int,
        closingDay: Int? = null,
        dueDay: Int? = null,
    ) {
        if (installments <= 1) {
            transactionDao.insert(base)
            return
        }
        val groupId = UUID.randomUUID().toString()
        val perInstallment = base.amount / installments
        val firstDate = DateUtils.toLocalDate(base.date)
        val list = (0 until installments).map { index ->
            val dateMillis = DateUtils.localDateToMillis(firstDate.plusMonths(index.toLong()))
            base.copy(
                amount = perInstallment,
                date = dateMillis,
                invoiceDate = invoiceDateFor(dateMillis, base.creditCardId, closingDay, dueDay),
                installmentNumber = index + 1,
                installmentTotal = installments,
                groupId = groupId,
            )
        }
        transactionDao.insertAll(list)
    }

    suspend fun recomputeCardInvoiceDates(cards: List<CreditCardEntity>) {
        if (cards.isEmpty()) return
        val byId = cards.associateBy { it.id }
        transactionDao.getAll().forEach { tx ->
            if (tx.type != TransactionType.EXPENSE || tx.creditCardId == null) return@forEach
            val card = byId[tx.creditCardId] ?: return@forEach
            val invoiceDate = invoiceDateFor(tx.date, tx.creditCardId, card.closingDay, card.dueDay)
            if (invoiceDate != tx.invoiceDate) {
                transactionDao.update(tx.copy(invoiceDate = invoiceDate))
            }
        }
    }

    private fun invoiceDateFor(date: Long, creditCardId: Long?, closingDay: Int?, dueDay: Int?): Long {
        if (creditCardId == null || closingDay == null || dueDay == null) return date
        val cycle = CardCycleCalculator.currentCycle(DateUtils.toLocalDate(date), closingDay, dueDay)
        return DateUtils.localDateToMillis(cycle.closingDate)
    }
}
