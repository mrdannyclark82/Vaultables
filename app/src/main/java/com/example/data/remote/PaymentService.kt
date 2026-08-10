package com.example.data.remote

import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.Response

data class EscrowPaymentRequest(
    val itemId: Long,
    val amountUsd: Double,
    val buyerName: String,
    val paymentMethod: String = "stripe"
)

data class EscrowPaymentResponse(
    val clientSecret: String,
    val ephemeralKey: String,
    val customerId: String,
    val publishableKey: String
)

interface PaymentService {
    @POST("/api/v1/escrow/create-intent")
    suspend fun createEscrowPaymentIntent(
        @Body request: EscrowPaymentRequest
    ): Response<EscrowPaymentResponse>
}
