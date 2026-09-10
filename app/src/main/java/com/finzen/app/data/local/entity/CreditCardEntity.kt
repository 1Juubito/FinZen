package com.finzen.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finzen.app.data.model.CardBrand

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val creditLimit: Double,
    val closingDay: Int,
    val dueDay: Int,
    val colorHex: String,
    val brand: CardBrand = CardBrand.OTHER,
)
