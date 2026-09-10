package com.finzen.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finzen.app.data.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Double = 0.0,
    val colorHex: String,
    val iconKey: String,
    val includeInTotal: Boolean = true,
    val archived: Boolean = false,
)
