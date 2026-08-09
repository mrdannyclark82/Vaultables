package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_reviews")
data class UserReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reviewerName: String,
    val rating: Float, // e.g. 5.0
    val comment: String,
    val dateText: String = "2 days ago",
    val verifiedPurchase: Boolean = true
)
