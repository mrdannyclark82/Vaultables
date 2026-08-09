package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_alerts")
data class TradeAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val alertType: String, // "OFFER", "ESCROW", "AUTH", "PRICE"
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
