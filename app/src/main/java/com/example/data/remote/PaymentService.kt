package com.example.data.remote

import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.Response

data class EscrowPaymentRequest(
    val listingId: String
)

data class EscrowPaymentResponse(
    val paymentIntentId: String,
    val clientSecret: String,
    val amountMinor: Long,
    val currency: String
)

interface PaymentService {
    @POST("/api/v1/escrow/create-intent")
    suspend fun createEscrowPaymentIntent(
        @Header("Authorization") authorization: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: EscrowPaymentRequest
    ): Response<EscrowPaymentResponse>
}
