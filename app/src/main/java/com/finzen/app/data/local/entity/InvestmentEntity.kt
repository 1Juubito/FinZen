package com.finzen.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finzen.app.data.model.InvestmentType

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: InvestmentType,
    val institution: String = "",

    val investedAmount: Double,

    val currentValue: Double,

    val date: Long? = null,
    val colorHex: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
