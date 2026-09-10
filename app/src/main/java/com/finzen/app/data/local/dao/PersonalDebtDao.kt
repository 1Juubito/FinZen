package com.finzen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.finzen.app.data.local.entity.PersonalDebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalDebtDao {

    @Query("SELECT * FROM personal_debts ORDER BY settled ASC, date DESC")
    fun observeAll(): Flow<List<PersonalDebtEntity>>

    @Upsert
    suspend fun upsert(debt: PersonalDebtEntity)

    @Query("UPDATE personal_debts SET settled = :settled, settledAt = :at WHERE id = :id")
    suspend fun setSettled(id: Long, settled: Boolean, at: Long?)

    @Delete
    suspend fun delete(debt: PersonalDebtEntity)

    @Query("SELECT * FROM personal_debts")
    suspend fun getAll(): List<PersonalDebtEntity>

    @Insert
    suspend fun insertAll(debts: List<PersonalDebtEntity>)

    @Query("DELETE FROM personal_debts")
    suspend fun deleteAll()
}
