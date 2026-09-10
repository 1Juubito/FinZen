package com.finzen.app.data.repository

import com.finzen.app.data.local.dao.AccountDao
import com.finzen.app.data.local.dao.TransactionDao
import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.model.AccountWithBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AccountRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
) {
    fun observeActive(): Flow<List<AccountEntity>> = accountDao.observeActive()
    fun observeById(id: Long): Flow<AccountEntity?> = accountDao.observeById(id)
    suspend fun getById(id: Long): AccountEntity? = accountDao.getById(id)
    suspend fun count(): Int = accountDao.count()

    suspend fun insert(account: AccountEntity): Long = accountDao.insert(account)
    suspend fun update(account: AccountEntity) = accountDao.update(account)
    suspend fun delete(account: AccountEntity) = accountDao.delete(account)

    fun observeAccountsWithBalance(): Flow<List<AccountWithBalance>> =
        combine(
            accountDao.observeActive(),
            transactionDao.observeAccountDeltas(),
            transactionDao.observeTransferInDeltas(),
        ) { accounts, deltas, transfersIn ->
            val deltaMap = deltas.associate { it.accountId to it.delta }
            val transferInMap = transfersIn.associate { it.accountId to it.delta }
            accounts.map { account ->
                val balance = account.initialBalance +
                    (deltaMap[account.id] ?: 0.0) +
                    (transferInMap[account.id] ?: 0.0)
                AccountWithBalance(account, balance)
            }
        }

    fun observeTotalBalance(): Flow<Double> =
        observeAccountsWithBalance().map { list ->
            list.filter { it.account.includeInTotal }.sumOf { it.balance }
        }
}
