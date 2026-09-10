package com.finzen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.finzen.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun observeById(id: Long): Flow<GoalEntity?>

    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Query("UPDATE goals SET savedAmount = MAX(0, savedAmount + :amount) WHERE id = :id")
    suspend fun addFunds(id: Long, amount: Double)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>

    @Insert
    suspend fun insertAll(goals: List<GoalEntity>)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
