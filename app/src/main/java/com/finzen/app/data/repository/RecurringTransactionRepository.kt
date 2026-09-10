package com.finzen.app.data.repository

import com.finzen.app.data.local.dao.RecurringTransactionDao
import com.finzen.app.data.local.entity.RecurringTransactionEntity
import com.finzen.app.data.recurrence.RecurrenceScheduler
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.Flow

class RecurringTransactionRepository(private val dao: RecurringTransactionDao) {

    fun observeAll(): Flow<List<RecurringTransactionEntity>> = dao.observeAll()

    fun observeActiveForTotals(): Flow<List<RecurringTransactionEntity>> = dao.observeActiveForTotals()

    suspend fun getDue(until: Long = DateUtils.now()): List<RecurringTransactionEntity> = dao.getDue(until)

    suspend fun getById(id: Long): RecurringTransactionEntity? = dao.getById(id)

    suspend fun upsert(item: RecurringTransactionEntity): Long = dao.upsert(item)

    suspend fun delete(item: RecurringTransactionEntity) = dao.delete(item)

    suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)

    suspend fun advance(id: Long) {
        val item = dao.getById(id) ?: return
        val current = DateUtils.toLocalDate(item.nextDueDate)
        val next = RecurrenceScheduler.advance(current, item.frequency, item.interval)
        val nextMillis = DateUtils.localDateToMillis(next)
        val stillActive = item.active && (item.endDate == null || nextMillis <= item.endDate)
        dao.updateSchedule(id, nextMillis, stillActive)
    }
}
