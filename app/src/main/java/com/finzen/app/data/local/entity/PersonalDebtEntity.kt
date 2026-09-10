package com.finzen.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finzen.app.data.model.DebtDirection

@Entity(tableName = "personal_debts")
data class PersonalDebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val amount: Double,
    val direction: DebtDirection,
    val date: Long,
    val dueDate: Long? = null,
    val notes: String? = null,
    val settled: Boolean = false,
    val settledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
