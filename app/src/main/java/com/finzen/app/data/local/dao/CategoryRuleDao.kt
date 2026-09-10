package com.finzen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.finzen.app.data.local.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Query("SELECT * FROM category_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CategoryRuleEntity>>

    @Insert
    suspend fun insert(rule: CategoryRuleEntity): Long

    @Delete
    suspend fun delete(rule: CategoryRuleEntity)

    @Query("SELECT * FROM category_rules")
    suspend fun getAll(): List<CategoryRuleEntity>

    @Insert
    suspend fun insertAll(rules: List<CategoryRuleEntity>)

    @Query("DELETE FROM category_rules")
    suspend fun deleteAll()
}
