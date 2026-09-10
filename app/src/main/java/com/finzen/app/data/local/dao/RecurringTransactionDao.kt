package com.finzen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.finzen.app.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Query("SELECT * FROM recurring_transactions ORDER BY active DESC, nextDueDate ASC")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    @Query(
        """
        SELECT * FROM recurring_transactions
        WHERE active = 1
        AND (accountId IS NULL OR accountId NOT IN (SELECT id FROM accounts WHERE includeInTotal = 0))
        ORDER BY nextDueDate ASC
        """
    )
    fun observeActiveForTotals(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE active = 1 AND nextDueDate <= :until")
    suspend fun getDue(until: Long): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransactionEntity?

    @Upsert
    suspend fun upsert(item: RecurringTransactionEntity): Long

    @Delete
    suspend fun delete(item: RecurringTransactionEntity)

    @Query("UPDATE recurring_transactions SET nextDueDate = :nextDueDate, active = :active WHERE id = :id")
    suspend fun updateSchedule(id: Long, nextDueDate: Long, active: Boolean)

    @Query("UPDATE recurring_transactions SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAll(): List<RecurringTransactionEntity>

    @Insert
    suspend fun insertAll(items: List<RecurringTransactionEntity>)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}
