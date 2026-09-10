package com.finzen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.finzen.app.data.local.entity.TransactionEntity
import com.finzen.app.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT t.*,
            c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon,
            a.name AS accountName, a.colorHex AS accountColor, a.iconKey AS accountIcon,
            ta.name AS toAccountName,
            cc.name AS cardName, cc.colorHex AS cardColor
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN accounts a ON t.accountId = a.id
        LEFT JOIN accounts ta ON t.toAccountId = ta.id
        LEFT JOIN credit_cards cc ON t.creditCardId = cc.id
        WHERE t.invoiceDate >= :start AND t.invoiceDate < :end
        ORDER BY t.date DESC, t.id DESC
        """
    )
    fun observeDetailsBetween(start: Long, end: Long): Flow<List<TransactionDetails>>

    @Query(
        """
        SELECT t.*,
            c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon,
            a.name AS accountName, a.colorHex AS accountColor, a.iconKey AS accountIcon,
            ta.name AS toAccountName,
            cc.name AS cardName, cc.colorHex AS cardColor
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN accounts a ON t.accountId = a.id
        LEFT JOIN accounts ta ON t.toAccountId = ta.id
        LEFT JOIN credit_cards cc ON t.creditCardId = cc.id
        ORDER BY t.date DESC, t.id DESC
        LIMIT :limit
        """
    )
    fun observeRecentDetails(limit: Int): Flow<List<TransactionDetails>>

    @Query(
        """
        SELECT t.*,
            c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon,
            a.name AS accountName, a.colorHex AS accountColor, a.iconKey AS accountIcon,
            ta.name AS toAccountName,
            cc.name AS cardName, cc.colorHex AS cardColor
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN accounts a ON t.accountId = a.id
        LEFT JOIN accounts ta ON t.toAccountId = ta.id
        LEFT JOIN credit_cards cc ON t.creditCardId = cc.id
        ORDER BY t.date DESC, t.id DESC
        """
    )
    fun observeAllDetails(): Flow<List<TransactionDetails>>

    @Query(
        """
        SELECT t.*,
            c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon,
            a.name AS accountName, a.colorHex AS accountColor, a.iconKey AS accountIcon,
            ta.name AS toAccountName,
            cc.name AS cardName, cc.colorHex AS cardColor
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN accounts a ON t.accountId = a.id
        LEFT JOIN accounts ta ON t.toAccountId = ta.id
        LEFT JOIN credit_cards cc ON t.creditCardId = cc.id
        WHERE t.installmentTotal IS NOT NULL AND t.invoiceDate >= :from
        ORDER BY t.invoiceDate ASC, t.date ASC
        """
    )
    fun observeFutureInstallments(from: Long): Flow<List<TransactionDetails>>

    @Query(
        """
        SELECT t.*,
            c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon,
            a.name AS accountName, a.colorHex AS accountColor, a.iconKey AS accountIcon,
            ta.name AS toAccountName,
            cc.name AS cardName, cc.colorHex AS cardColor
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN accounts a ON t.accountId = a.id
        LEFT JOIN accounts ta ON t.toAccountId = ta.id
        LEFT JOIN credit_cards cc ON t.creditCardId = cc.id
        WHERE t.isPaid = 0 AND t.creditCardId IS NULL
        AND (t.accountId IS NULL OR t.accountId NOT IN (SELECT id FROM accounts WHERE includeInTotal = 0))
        ORDER BY t.date ASC, t.id ASC
        """
    )
    fun observePendingDetails(): Flow<List<TransactionDetails>>

    @Query(
        """
        SELECT t.*,
            c.name AS categoryName, c.colorHex AS categoryColor, c.iconKey AS categoryIcon,
            a.name AS accountName, a.colorHex AS accountColor, a.iconKey AS accountIcon,
            ta.name AS toAccountName,
            cc.name AS cardName, cc.colorHex AS cardColor
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN accounts a ON t.accountId = a.id
        LEFT JOIN accounts ta ON t.toAccountId = ta.id
        LEFT JOIN credit_cards cc ON t.creditCardId = cc.id
        WHERE t.creditCardId = :cardId AND t.date >= :start AND t.date < :end
        ORDER BY t.date DESC, t.id DESC
        """
    )
    fun observeCardDetails(cardId: Long, start: Long, end: Long): Flow<List<TransactionDetails>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT date AS date, amount AS amount FROM transactions
        WHERE type = 'EXPENSE' AND date >= :start AND date < :end
        AND (accountId IS NULL OR accountId NOT IN (SELECT id FROM accounts WHERE includeInTotal = 0))
        ORDER BY date ASC
        """
    )
    fun observeExpensePoints(start: Long, end: Long): Flow<List<AmountPoint>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = :type AND invoiceDate >= :start AND invoiceDate < :end
        AND (accountId IS NULL OR accountId NOT IN (SELECT id FROM accounts WHERE includeInTotal = 0))
        """
    )
    fun observeTotalByType(type: TransactionType, start: Long, end: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = :type AND isPaid = 0 AND creditCardId IS NULL
        AND date >= :start AND date < :end
        AND (accountId IS NULL OR accountId NOT IN (SELECT id FROM accounts WHERE includeInTotal = 0))
        """
    )
    fun observePendingTotalByType(type: TransactionType, start: Long, end: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = :type AND isPaid = 0
        AND invoiceDate >= :start AND invoiceDate < :end
        AND (accountId IS NULL OR accountId NOT IN (SELECT id FROM accounts WHERE includeInTotal = 0))
        """
    )
    fun observePendingByInvoiceMonth(type: TransactionType, start: Long, end: Long): Flow<Double>

    @Query(
        """
        SELECT
            (SELECT COALESCE(SUM(initialBalance), 0) FROM accounts
                WHERE includeInTotal = 1 AND archived = 0)
          + (SELECT COALESCE(SUM(CASE t.type
                WHEN 'INCOME' THEN t.amount
                WHEN 'EXPENSE' THEN -t.amount
                WHEN 'TRANSFER' THEN -t.amount
                ELSE 0 END), 0)
             FROM transactions t INNER JOIN accounts a ON t.accountId = a.id
             WHERE t.isPaid = 1 AND t.creditCardId IS NULL
               AND a.includeInTotal = 1 AND a.archived = 0 AND t.date < :asOf)
          + (SELECT COALESCE(SUM(t.amount), 0)
             FROM transactions t INNER JOIN accounts a ON t.toAccountId = a.id
             WHERE t.type = 'TRANSFER' AND t.isPaid = 1
               AND a.includeInTotal = 1 AND a.archived = 0 AND t.date < :asOf)
        """
    )
    fun observeBalanceAsOf(asOf: Long): Flow<Double>

    @Query(
        """
        SELECT (SELECT COALESCE(SUM(CASE
                    WHEN type = 'INCOME' THEN amount
                    WHEN type = 'EXPENSE' THEN -amount
                    WHEN type = 'TRANSFER' THEN -amount
                    ELSE 0 END), 0)
                FROM transactions
                WHERE accountId = :accountId AND isPaid = 1 AND creditCardId IS NULL AND date < :asOf)
             + (SELECT COALESCE(SUM(amount), 0)
                FROM transactions
                WHERE type = 'TRANSFER' AND toAccountId = :accountId AND isPaid = 1 AND date < :asOf)
        """
    )
    fun observeAccountBalanceAsOf(accountId: Long, asOf: Long): Flow<Double>

    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, c.colorHex AS categoryColor,
               c.iconKey AS categoryIcon, COALESCE(SUM(t.amount), 0) AS total
        FROM transactions t JOIN categories c ON t.categoryId = c.id
        WHERE t.type = :type AND t.invoiceDate >= :start AND t.invoiceDate < :end
        AND (t.accountId IS NULL OR t.accountId NOT IN (SELECT id FROM accounts WHERE includeInTotal = 0))
        GROUP BY c.id
        ORDER BY total DESC
        """
    )
    fun observeSpendingByCategory(
        type: TransactionType,
        start: Long,
        end: Long
    ): Flow<List<CategorySpending>>

    @Query(
        """
        SELECT accountId AS accountId,
            COALESCE(SUM(CASE
                WHEN type = 'INCOME' THEN amount
                WHEN type = 'EXPENSE' THEN -amount
                WHEN type = 'TRANSFER' THEN -amount
                ELSE 0 END), 0) AS delta
        FROM transactions
        WHERE accountId IS NOT NULL AND isPaid = 1 AND creditCardId IS NULL
        GROUP BY accountId
        """
    )
    fun observeAccountDeltas(): Flow<List<AccountDelta>>

    @Query(
        """
        SELECT toAccountId AS accountId, COALESCE(SUM(amount), 0) AS delta
        FROM transactions
        WHERE type = 'TRANSFER' AND toAccountId IS NOT NULL AND isPaid = 1
        GROUP BY toAccountId
        """
    )
    fun observeTransferInDeltas(): Flow<List<AccountDelta>>

    @Query(
        """
        SELECT creditCardId AS creditCardId, COALESCE(SUM(amount), 0) AS total
        FROM transactions
        WHERE creditCardId IS NOT NULL AND type = 'EXPENSE' AND isPaid = 0
        GROUP BY creditCardId
        """
    )
    fun observeOpenCardTotals(): Flow<List<CardSpent>>

    @Query(
        """
        SELECT creditCardId AS creditCardId, invoiceDate AS invoiceDate, COALESCE(SUM(amount), 0) AS total
        FROM transactions
        WHERE creditCardId IS NOT NULL AND type = 'EXPENSE' AND isPaid = 0
        GROUP BY creditCardId, invoiceDate
        """
    )
    fun observeOpenCardInvoiceTotals(): Flow<List<CardInvoiceTotal>>

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("UPDATE transactions SET isPaid = :paid WHERE id = :id")
    suspend fun setPaid(id: Long, paid: Boolean)

    @Query(
        """
        UPDATE transactions SET isPaid = :paid
        WHERE creditCardId = :cardId AND type = 'EXPENSE' AND date >= :start AND date < :end
        """
    )
    suspend fun setCardInvoicePaid(cardId: Long, start: Long, end: Long, paid: Boolean)

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE creditCardId = :cardId AND type = 'EXPENSE' AND isPaid = 0
        AND date >= :start AND date < :end
        """
    )
    suspend fun unpaidCardTotal(cardId: Long, start: Long, end: Long): Double

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>
}
