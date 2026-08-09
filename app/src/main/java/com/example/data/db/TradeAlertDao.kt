package com.example.data.db

import androidx.room.*
import com.example.data.model.TradeAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeAlertDao {
    @Query("SELECT * FROM trade_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<TradeAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: TradeAlert): Long

    @Query("UPDATE trade_alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE trade_alerts SET isRead = 1")
    suspend fun markAllAsRead()
}
