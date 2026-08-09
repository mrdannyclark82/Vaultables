package com.example.data.db

import androidx.room.*
import com.example.data.model.CollectibleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectibleDao {
    @Query("SELECT * FROM collectible_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<CollectibleItem>>

    @Query("SELECT * FROM collectible_items WHERE isListedForSale = 1 ORDER BY createdAt DESC")
    fun getMarketplaceListings(): Flow<List<CollectibleItem>>

    @Query("SELECT * FROM collectible_items WHERE id = :id")
    suspend fun getItemById(id: Long): CollectibleItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CollectibleItem): Long

    @Update
    suspend fun updateItem(item: CollectibleItem)

    @Delete
    suspend fun deleteItem(item: CollectibleItem)

    @Query("DELETE FROM collectible_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM collectible_items")
    suspend fun deleteAllItems()
}
