package com.finzen.app.data.repository

import com.finzen.app.data.card.CardCycleCalculator
import com.finzen.app.data.local.dao.CreditCardDao
import com.finzen.app.data.local.dao.TransactionDao
import com.finzen.app.data.local.entity.CreditCardEntity
import com.finzen.app.data.model.CardWithUsage
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class CreditCardRepository(
    private val creditCardDao: CreditCardDao,
    private val transactionDao: TransactionDao,
) {
    fun observeAll(): Flow<List<CreditCardEntity>> = creditCardDao.observeAll()
    fun observeById(id: Long): Flow<CreditCardEntity?> = creditCardDao.observeById(id)
    suspend fun getById(id: Long): CreditCardEntity? = creditCardDao.getById(id)
    suspend fun getAll(): List<CreditCardEntity> = creditCardDao.getAll()

    suspend fun insert(card: CreditCardEntity): Long = creditCardDao.insert(card)
    suspend fun update(card: CreditCardEntity) = creditCardDao.update(card)
    suspend fun delete(card: CreditCardEntity) = creditCardDao.delete(card)

    fun observeCardsWithUsage(): Flow<List<CardWithUsage>> =
        combine(
            creditCardDao.observeAll(),
            transactionDao.observeOpenCardTotals(),
            transactionDao.observeOpenCardInvoiceTotals(),
        ) { cards, totals, invoiceTotals ->
            val usedMap = totals.associate { it.creditCardId to it.total }
            val today = LocalDate.now()
            cards.map { card ->
                val currentClosing = DateUtils.localDateToMillis(
                    CardCycleCalculator.currentCycle(today, card.closingDay, card.dueDay).closingDate,
                )
                val currentInvoice = invoiceTotals
                    .filter { it.creditCardId == card.id && it.invoiceDate == currentClosing }
                    .sumOf { it.total }
                CardWithUsage(card, used = usedMap[card.id] ?: 0.0, currentInvoice = currentInvoice)
            }
        }
}
