package com.finzen.app.data.repository

import com.finzen.app.data.local.dao.CategoryRuleDao
import com.finzen.app.data.local.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

class CategoryRuleRepository(private val dao: CategoryRuleDao) {

    fun observeAll(): Flow<List<CategoryRuleEntity>> = dao.observeAll()

    suspend fun insert(rule: CategoryRuleEntity): Long = dao.insert(rule)

    suspend fun delete(rule: CategoryRuleEntity) = dao.delete(rule)
}
