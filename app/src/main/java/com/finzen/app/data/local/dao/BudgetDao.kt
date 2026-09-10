package com.finzen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.finzen.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month")
    fun observeForMonth(year: Int, month: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND year = :year AND month = :month LIMIT 1")
    suspend fun getForCategory(categoryId: Long, year: Int, month: Int): BudgetEntity?

    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Insert
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
