package com.finzen.app.data.repository

import com.finzen.app.data.local.dao.InvestmentDao
import com.finzen.app.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

class InvestmentRepository(private val investmentDao: InvestmentDao) {
    fun observeAll(): Flow<List<InvestmentEntity>> = investmentDao.observeAll()
    fun observeById(id: Long): Flow<InvestmentEntity?> = investmentDao.observeById(id)

    suspend fun upsert(investment: InvestmentEntity) = investmentDao.upsert(investment)
    suspend fun contribute(id: Long, amount: Double) = investmentDao.contribute(id, amount)
    suspend fun updateValue(id: Long, value: Double) = investmentDao.updateValue(id, value)
    suspend fun addYield(id: Long, amount: Double) = investmentDao.addYield(id, amount)
    suspend fun delete(investment: InvestmentEntity) = investmentDao.delete(investment)
}
