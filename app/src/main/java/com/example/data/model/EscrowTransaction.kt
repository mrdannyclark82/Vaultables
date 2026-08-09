package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EscrowStatus(val label: String, val stepIndex: Int) {
    INITIATED("Escrow Created", 0),
    FUNDS_HELD("Funds Secured in Escrow", 1),
    SHIPPED("Item Shipped (Tracking active)", 2),
    VERIFICATION("In Inspection & AI Verification", 3),
    RELEASED("Funds Released & Trade Complete", 4)
}

enum class CurrencyCode(val symbol: String, val code: String, val rateFromUsd: Double) {
    USD("$", "USD", 1.0),
    EUR("€", "EUR", 0.92),
    GBP("£", "GBP", 0.78),
    JPY("¥", "JPY", 155.0)
}

@Entity(tableName = "escrow_transactions")
data class EscrowTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val itemTitle: String,
    val buyerName: String,
    val sellerName: String,
    val amountUsd: Double,
    val currencyCode: String = "USD",
    val feeUsd: Double, // platform fee
    val feePercentage: Double = 3.5, // configurable fee % (3.0 - 5.0)
    val buyerConfirmed: Boolean = false,
    val sellerConfirmed: Boolean = false,
    val status: String = EscrowStatus.FUNDS_HELD.name,
    val trackingNumber: String = "TRK-VAULT-${(10000..99999).random()}",
    val shippingCarrier: String = "VaultX Secure Logistics",
    val createdAt: Long = System.currentTimeMillis()
)
