package com.finzen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.finzen.app.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {

    @Query("SELECT * FROM investments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE id = :id")
    fun observeById(id: Long): Flow<InvestmentEntity?>

    @Upsert
    suspend fun upsert(investment: InvestmentEntity)

    @Query("UPDATE investments SET investedAmount = investedAmount + :amount, currentValue = currentValue + :amount WHERE id = :id")
    suspend fun contribute(id: Long, amount: Double)

    @Query("UPDATE investments SET currentValue = :value WHERE id = :id")
    suspend fun updateValue(id: Long, value: Double)

    @Query("UPDATE investments SET currentValue = currentValue + :amount WHERE id = :id")
    suspend fun addYield(id: Long, amount: Double)

    @Delete
    suspend fun delete(investment: InvestmentEntity)

    @Query("SELECT * FROM investments")
    suspend fun getAll(): List<InvestmentEntity>

    @Insert
    suspend fun insertAll(investments: List<InvestmentEntity>)

    @Query("DELETE FROM investments")
    suspend fun deleteAll()
}
