package com.example.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class CardScanImage(
    val mimeType: String,
    val dataBase64: String
)

data class SecureScannerRequest(
    val images: CardScanImages,
    val category: String,
    val notes: String
)

data class CardScanImages(
    val front: CardScanImage,
    val back: CardScanImage
)

interface ScannerService {
    @POST("/api/v1/scanner/analyze")
    suspend fun analyzeCollectible(
        @Header("Authorization") authorization: String,
        @Body request: SecureScannerRequest
    ): Response<ResponseBody>
}
