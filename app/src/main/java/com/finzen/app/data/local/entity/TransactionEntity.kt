package com.finzen.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finzen.app.data.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditCardId"],
            onDelete = ForeignKey.SET_NULL
        ),
    ],
    indices = [
        Index("categoryId"), Index("accountId"), Index("toAccountId"),
        Index("creditCardId"), Index("date")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val date: Long,

    @ColumnInfo(defaultValue = "0") val invoiceDate: Long = date,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val creditCardId: Long? = null,
    val isPaid: Boolean = true,
    val notes: String? = null,
    val installmentNumber: Int? = null,
    val installmentTotal: Int? = null,
    val groupId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
