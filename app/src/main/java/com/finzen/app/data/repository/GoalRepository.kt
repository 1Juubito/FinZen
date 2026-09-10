package com.finzen.app.data.repository

import com.finzen.app.data.local.dao.GoalDao
import com.finzen.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    fun observeAll(): Flow<List<GoalEntity>> = goalDao.observeAll()
    fun observeById(id: Long): Flow<GoalEntity?> = goalDao.observeById(id)

    suspend fun upsert(goal: GoalEntity) = goalDao.upsert(goal)
    suspend fun addFunds(id: Long, amount: Double) = goalDao.addFunds(id, amount)
    suspend fun delete(goal: GoalEntity) = goalDao.delete(goal)
}
