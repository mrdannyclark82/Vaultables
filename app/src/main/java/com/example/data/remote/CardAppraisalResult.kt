package com.example.data.remote

data class CardAppraisalResult(
    val detectedTitle: String = "",
    val detectedName: String = "",
    val detectedTeam: String = "",
    val detectedBrand: String = "",
    val detectedYear: String = "",
    val detectedCardNumber: String = "",
    val grade: String,
    val certSerialNumber: String,
    val gradingCompany: String,
    val centeringGrade: Float,
    val cornersGrade: Float,
    val edgesGrade: Float,
    val surfaceGrade: Float,
    val authenticityScore: Int,
    val estimatedValueUsd: Double,
    val marketTrend: String,
    val highlights: List<String>,
    val vaultHashId: String,
    val fullAnalysis: String
)
