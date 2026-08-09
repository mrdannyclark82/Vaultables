package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CollectibleCategory(val displayName: String, val iconName: String) {
    CARDS("Trading Cards", "Style"),
    COMICS("Comic Books", "Book"),
    WATCHES("Luxury Watches", "Watch"),
    SNEAKERS("Sneakers", "DirectionsRun"),
    COINS("Coins & Bullion", "MonetizationOn"),
    ART("Fine Art", "Palette"),
    FIGURINES("Figurines & Toys", "Toys")
}

@Entity(tableName = "collectible_items")
data class CollectibleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // CollectibleCategory.name
    val description: String,
    val ownerName: String = "Vault Collector",
    val ownerRating: Float = 4.9f,
    val estimatedValueUsd: Double,
    val conditionGrade: String, // e.g. "9.8 Gem Mint", "PSA 10", "9.4 Near Mint"
    val authenticityScore: Int = 98, // 0 - 100%
    val vaultHashId: String, // Unique identifier e.g. "VAULT-8F3A-92D1-2026"
    val isListedForSale: Boolean = false,
    val salePriceUsd: Double = 0.0,
    val isEscrowActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = true,
    val imageType: String = "CARD" // "CARD", "WATCH", "COMIC", "SNEAKER", "COIN", "ART"
)
