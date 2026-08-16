package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*

@Database(
    entities = [
        CollectibleItem::class,
        EscrowTransaction::class,
        ChatMessage::class,
        TradeAlert::class,
        UserReview::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectibleDao(): CollectibleDao
    abstract fun escrowDao(): EscrowDao
    abstract fun messageDao(): MessageDao
    abstract fun tradeAlertDao(): TradeAlertDao
    abstract fun userReviewDao(): UserReviewDao

    companion object {
        private val MIGRATION_6_8 = object : Migration(6, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE collectible_items ADD COLUMN localBackImagePath TEXT")
                database.execSQL("ALTER TABLE collectible_items ADD COLUMN verificationSummary TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE collectible_items ADD COLUMN teamName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE collectible_items ADD COLUMN cardNumber TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vault_collectibles.db"
                )
                .addMigrations(MIGRATION_6_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
