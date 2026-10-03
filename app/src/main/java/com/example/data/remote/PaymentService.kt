package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class EscrowPaymentRequest(
    val listingId: String
)

data class EscrowPaymentResponse(
    val paymentIntentId: String,
    val clientSecret: String,
    val amountMinor: Long,
    val currency: String
)

data class EscrowShipRequest(
    val carrier: String,
    val trackingNumber: String
)

data class EscrowDisputeRequest(
    val reason: String,
    val details: String? = null
)

data class EscrowActionResponse(
    val escrowId: String,
    val state: String,
    val releaseStatus: String,
    val buyerConfirmedReceived: Boolean = false,
    val sellerConfirmedShipped: Boolean = false,
    val payoutNotice: String? = null,
    val disputeId: String? = null
)

data class ConnectAccountLinkRequest(
    val returnUrl: String,
    val refreshUrl: String
)

data class ConnectAccountLinkResponse(
    val accountId: String,
    val url: String,
    val expiresAt: Long
)

data class ConnectStatusResponse(
    val connected: Boolean,
    val accountId: String? = null,
    val detailsSubmitted: Boolean = false,
    val chargesEnabled: Boolean = false,
    val payoutsEnabled: Boolean = false
)

interface PaymentService {
    @POST("/api/v1/escrow/create-intent")
    suspend fun createEscrowPaymentIntent(
        @Header("Authorization") authorization: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: EscrowPaymentRequest
    ): Response<EscrowPaymentResponse>

    @POST("/api/v1/escrow/{escrowId}/ship")
    suspend fun shipEscrow(
        @Header("Authorization") authorization: String,
        @Path("escrowId") escrowId: String,
        @Body request: EscrowShipRequest
    ): Response<EscrowActionResponse>

    @POST("/api/v1/escrow/{escrowId}/confirm-receipt")
    suspend fun confirmEscrowReceipt(
        @Header("Authorization") authorization: String,
        @Path("escrowId") escrowId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<EscrowActionResponse>

    @POST("/api/v1/escrow/{escrowId}/disputes")
    suspend fun openEscrowDispute(
        @Header("Authorization") authorization: String,
        @Path("escrowId") escrowId: String,
        @Body request: EscrowDisputeRequest
    ): Response<EscrowActionResponse>

    @POST("/api/v1/connect/account-link")
    suspend fun createConnectAccountLink(
        @Header("Authorization") authorization: String,
        @Body request: ConnectAccountLinkRequest
    ): Response<ConnectAccountLinkResponse>

    @POST("/api/v1/connect/status")
    suspend fun getConnectStatus(
        @Header("Authorization") authorization: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ConnectStatusResponse>
}
