package com.finzen.app.data.repository

import com.finzen.app.data.local.dao.CategoryDao
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.model.CategoryType
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>> = categoryDao.observeByType(type)
    suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun insert(category: CategoryEntity): Long = categoryDao.insert(category)
    suspend fun update(category: CategoryEntity) = categoryDao.update(category)
    suspend fun delete(category: CategoryEntity) = categoryDao.delete(category)
}
