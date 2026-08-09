package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tradeId: Long = 0,
    val senderName: String,
    val receiverName: String,
    val encryptedText: String, // Encrypted hex string representation
    val decryptedText: String,
    val isSender: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
