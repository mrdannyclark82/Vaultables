package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EscrowStatus(val label: String, val stepIndex: Int) {
    INITIATED("Escrow Created", 0),
    FUNDS_HELD("Funds Secured in Escrow", 1),
    PAID("Payment Secured", 1),
    SHIPPED("Item Shipped (Tracking active)", 2),
    VERIFICATION("In Inspection & AI Verification", 3),
    INSPECTION("In Inspection", 3),
    RELEASE_PROCESSING("Release Processing", 3),
    RELEASED("Funds Released & Trade Complete", 4),
    DISPUTED("Dispute Open", 2);

    companion object {
        fun fromServerState(state: String): EscrowStatus = when (state.lowercase()) {
            "paid" -> PAID
            "shipped" -> SHIPPED
            "inspection" -> INSPECTION
            "release_processing" -> RELEASE_PROCESSING
            "released" -> RELEASED
            "disputed" -> DISPUTED
            else -> FUNDS_HELD
        }
    }
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
    val feeUsd: Double, // platform fee (display only; server is authoritative for settlement)
    val feePercentage: Double = 3.5,
    val buyerConfirmed: Boolean = false,
    val sellerConfirmed: Boolean = false,
    val status: String = EscrowStatus.PAID.name,
    val trackingNumber: String = "",
    val shippingCarrier: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** Server escrow document id (Stripe paymentIntent id). Empty until webhook creates it. */
    val serverEscrowId: String = "",
    val listingId: String = "",
    val releaseStatus: String = "held",
    val payoutNotice: String = ""
)
