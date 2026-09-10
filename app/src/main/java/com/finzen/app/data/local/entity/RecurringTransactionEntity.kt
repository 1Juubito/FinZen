package com.finzen.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finzen.app.data.model.RecurrenceFrequency
import com.finzen.app.data.model.TransactionType

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId"), Index("accountId"), Index("creditCardId")],
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val creditCardId: Long? = null,
    val notes: String? = null,
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val startDate: Long,
    val endDate: Long? = null,
    val nextDueDate: Long,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
