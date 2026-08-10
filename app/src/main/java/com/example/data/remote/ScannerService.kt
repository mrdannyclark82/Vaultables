package com.example.data.remote

import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.Response

data class ScannerRequest(
    val imageBase64: String,
    val category: String,
    val notes: String
)

data class ScannerResponse(
    val detectedTitle: String = "",
    val detectedName: String = "",
    val detectedBrand: String = "",
    val detectedYear: String = "",
    val grade: String = "",
    val certSerialNumber: String = "",
    val gradingCompany: String = "",
    val centeringGrade: Float = 10.0f,
    val cornersGrade: Float = 10.0f,
    val edgesGrade: Float = 10.0f,
    val surfaceGrade: Float = 10.0f,
    val authenticityScore: Int = 100,
    val estimatedValueUsd: Double = 0.0,
    val marketTrend: String = "",
    val highlights: List<String> = emptyList(),
    val vaultHashId: String = "",
    val fullAnalysis: String = ""
)

interface ScannerService {
    @POST("/api/v1/scanner/analyze")
    suspend fun analyzeCollectible(
        @Body request: ScannerRequest
    ): Response<ScannerResponse>
}
