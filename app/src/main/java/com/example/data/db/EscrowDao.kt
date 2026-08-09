package com.example.data.db

import androidx.room.*
import com.example.data.model.EscrowTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface EscrowDao {
    @Query("SELECT * FROM escrow_transactions ORDER BY createdAt DESC")
    fun getAllEscrows(): Flow<List<EscrowTransaction>>

    @Query("SELECT * FROM escrow_transactions WHERE id = :id")
    suspend fun getEscrowById(id: Long): EscrowTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEscrow(escrow: EscrowTransaction): Long

    @Update
    suspend fun updateEscrow(escrow: EscrowTransaction)
}
