package com.finzen.app.data.repository

import com.finzen.app.data.local.dao.PersonalDebtDao
import com.finzen.app.data.local.entity.PersonalDebtEntity
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.Flow

class PersonalDebtRepository(private val dao: PersonalDebtDao) {
    fun observeAll(): Flow<List<PersonalDebtEntity>> = dao.observeAll()

    suspend fun upsert(debt: PersonalDebtEntity) = dao.upsert(debt)

    suspend fun delete(debt: PersonalDebtEntity) = dao.delete(debt)

    suspend fun setSettled(id: Long, settled: Boolean) =
        dao.setSettled(id, settled, if (settled) DateUtils.now() else null)
}
