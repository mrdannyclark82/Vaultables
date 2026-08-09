package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        CollectibleItem::class,
        EscrowTransaction::class,
        ChatMessage::class,
        TradeAlert::class,
        UserReview::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectibleDao(): CollectibleDao
    abstract fun escrowDao(): EscrowDao
    abstract fun messageDao(): MessageDao
    abstract fun tradeAlertDao(): TradeAlertDao
    abstract fun userReviewDao(): UserReviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vault_collectibles.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
