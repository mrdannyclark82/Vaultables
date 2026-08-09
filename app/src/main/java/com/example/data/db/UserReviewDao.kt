package com.example.data.db

import androidx.room.*
import com.example.data.model.UserReview
import kotlinx.coroutines.flow.Flow

@Dao
interface UserReviewDao {
    @Query("SELECT * FROM user_reviews ORDER BY id DESC")
    fun getAllReviews(): Flow<List<UserReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: UserReview): Long
}
