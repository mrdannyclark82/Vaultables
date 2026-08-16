package com.example.data.remote

import org.json.JSONObject

data class SecureScanResult(
    val title: String = "",
    val brand: String = "",
    val set: String = "",
    val year: String = "",
    val cardNumber: String = "",
    val grade: String = "",
    val gradingCompany: String = "",
    val certSerialNumber: String = "",
    val observations: List<String> = emptyList(),
    val verificationSummary: String = "",
    val notices: List<String> = emptyList()
)

object SecureScanResultParser {
    fun parse(body: String): SecureScanResult {
        val root = JSONObject(body)
        val extracted = root.optJSONObject("extracted")
            ?: throw IllegalArgumentException("The scan service returned no extracted card data.")
        val identity = extracted.optJSONObject("identity") ?: JSONObject()
        val certification = extracted.optJSONObject("visibleCertification") ?: JSONObject()
        val observations = extracted.optJSONArray("conditionObservations")
            ?.let { values -> List(values.length()) { index -> values.optString(index) }.filter(String::isNotBlank) }
            .orEmpty()
        val notices = root.optJSONArray("notices")
            ?.let { values ->
                List(values.length()) { index ->
                    values.optJSONObject(index)?.optString("message").orEmpty()
                }.filter(String::isNotBlank)
            }
            .orEmpty()
        val providers = root.optJSONObject("providers")
        val providerSummary = listOf("gemini", "cardsight", "googleCustomSearch")
            .mapNotNull { provider ->
                providers?.optJSONObject(provider)?.optString("status")?.takeIf(String::isNotBlank)
                    ?.let { "$provider: $it" }
            }
            .joinToString(" | ")

        return SecureScanResult(
            title = identity.optString("title"),
            brand = identity.optString("brand"),
            set = identity.optString("set"),
            year = identity.optString("year"),
            cardNumber = identity.optString("cardNumber"),
            grade = certification.optString("grade"),
            gradingCompany = certification.optString("company"),
            certSerialNumber = certification.optString("serialNumber"),
            observations = observations,
            verificationSummary = providerSummary,
            notices = notices
        )
    }
}
