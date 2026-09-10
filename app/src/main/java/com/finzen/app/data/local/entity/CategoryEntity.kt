package com.finzen.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finzen.app.data.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val colorHex: String,
    val iconKey: String,
    val isDefault: Boolean = false,
)
