package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CollectibleCategory(
    val displayName: String,
    val iconName: String,
    val subcategories: List<String> = emptyList()
) {
    TRADING_CARDS(
        "Trading Cards",
        "Style",
        listOf("All Sports", "Basketball", "Baseball", "Football", "NASCAR", "UFC", "Soccer", "F1", "Hockey")
    ),
    POKEMON_CARDS(
        "Pokémon & TCG",
        "AutoAwesome",
        listOf("All TCG", "Pokémon", "Magic: The Gathering", "Yu-Gi-Oh!", "One Piece", "Lorcana")
    ),
    DIECAST(
        "Diecast & Models",
        "DirectionsCar",
        listOf("All Diecast", "Hot Wheels", "Matchbox", "AutoArt", "Kyosho", "Formula 1 Models")
    ),
    CLOTHING(
        "Apparel & Streetwear",
        "Checkroom",
        listOf("All Streetwear", "Supreme", "Off-White", "Vintage Jerseys", "Designer Kicks", "High Fashion")
    ),
    TRENDING(
        "Trending & Pop Culture",
        "Whatshot",
        listOf("All Viral", "Viral Dumplings", "Squishies", "Funko Pops", "Labubu", "Vinyl Art Toys")
    ),
    COMICS(
        "Comic Books",
        "Book",
        listOf("All Comics", "Marvel", "DC Comics", "Manga", "Indie / Silver Age")
    ),
    WATCHES(
        "Luxury Watches",
        "Watch",
        listOf("All Watches", "Rolex", "Patek Philippe", "Audemars Piguet", "Omega")
    ),
    SNEAKERS(
        "Sneakers",
        "DirectionsRun",
        listOf("All Sneakers", "Air Jordan", "Yeezy", "Nike SB", "Travis Scott")
    ),
    COINS(
        "Coins & Bullion",
        "MonetizationOn",
        listOf("All Bullion", "Gold Coins", "Silver Eagles", "Ancient Currency")
    ),
    ART(
        "Fine Art",
        "Palette",
        listOf("All Art", "Modern Art", "Prints", "Sculptures")
    ),
    FIGURINES(
        "Figurines & Toys",
        "Toys",
        listOf("All Toys", "Action Figures", "Statues", "Nendoroid")
    )
}

@Entity(tableName = "collectible_items")
data class CollectibleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // CollectibleCategory.displayName
    val subcategory: String = "", // e.g. "Basketball", "NASCAR", "UFC", "Pokémon", "Viral Dumplings", "Squishies", "Funko Pops", "Hot Wheels", "Supreme"
    val description: String,
    val ownerName: String = "Vault Collector",
    val ownerRating: Float = 4.9f,
    val estimatedValueUsd: Double,
    val conditionGrade: String, // e.g. "9.8 Gem Mint", "PSA 10", "9.4 Near Mint"
    val certSerialNumber: String = "CERT-84920194", // Unique Beckett/PSA-style serial cert number e.g. "PSA-84920194" or "BGS-00129481"
    val gradingCompany: String = "PSA", // e.g. "PSA", "BGS", "CGC", "SGC", "VAULT AI"
    val centeringGrade: Float = 9.5f,
    val cornersGrade: Float = 10.0f,
    val edgesGrade: Float = 9.5f,
    val surfaceGrade: Float = 9.5f,
    val authenticityScore: Int = 98, // 0 - 100%
    val vaultHashId: String, // Unique identifier e.g. "VAULT-8F3A-92D1-2026"
    val isListedForSale: Boolean = false,
    val salePriceUsd: Double = 0.0,
    val isEscrowActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = true,
    val imageType: String = "CARD", // "CARD", "WATCH", "COMIC", "SNEAKER", "COIN", "ART", "DIECAST", "CLOTHING", "TRENDING"
    val brandName: String = "", // e.g. "Fleer", "Topps", "Rolex", "Nike"
    val releaseYear: String = "", // e.g. "1986", "1999", "2020"
    val teamName: String = "",
    val cardNumber: String = "",
    val localImagePath: String? = null, // local URI if a front photo was taken
    val localBackImagePath: String? = null,
    val verificationSummary: String = ""
)
