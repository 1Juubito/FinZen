package com.finzen.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.finzen.app.data.local.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    @Query("SELECT * FROM credit_cards ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    fun observeById(id: Long): Flow<CreditCardEntity?>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getById(id: Long): CreditCardEntity?

    @Insert
    suspend fun insert(card: CreditCardEntity): Long

    @Update
    suspend fun update(card: CreditCardEntity)

    @Delete
    suspend fun delete(card: CreditCardEntity)

    @Query("SELECT * FROM credit_cards")
    suspend fun getAll(): List<CreditCardEntity>

    @Insert
    suspend fun insertAll(cards: List<CreditCardEntity>)

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAll()
}
