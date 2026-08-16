package com.example.data.remote

import org.json.JSONObject

data class ScanDraft(
    val title: String,
    val category: String,
    val description: String,
    val imageType: String,
    val brand: String,
    val year: String,
    val cardNumber: String,
    val grade: String,
    val gradingCompany: String,
    val certSerialNumber: String,
    val localImagePath: String?,
    val localBackImagePath: String?,
    val verificationSummary: String,
    val notices: List<String>,
    val observations: List<String>
)

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
        val extracted = root.optJSONObject("extracted") ?: JSONObject()
        val identity = extracted.optJSONObject("identity") ?: JSONObject()
        val certification = extracted.optJSONObject("visibleCertification") ?: JSONObject()
        val observations = extracted.optJSONArray("conditionObservations")
            ?.let { values -> List(values.length()) { index -> clean(values.opt(index)) }.filter(String::isNotBlank) }
            .orEmpty()
        val notices = root.optJSONArray("notices")
            ?.let { values ->
                List(values.length()) { index ->
                    clean(values.optJSONObject(index)?.opt("message"))
                }.filter(String::isNotBlank)
            }
            .orEmpty()
        val providers = root.optJSONObject("providers")
        val providerSummary = listOf("gemini", "cardsight", "googleCustomSearch")
            .mapNotNull { provider ->
                clean(providers?.optJSONObject(provider)?.opt("status"))
                    .takeIf(String::isNotBlank)
                    ?.let { "$provider: $it" }
            }
            .joinToString(" | ")

        return SecureScanResult(
            title = clean(identity.opt("title")),
            brand = clean(identity.opt("brand")),
            set = clean(identity.opt("set")),
            year = clean(identity.opt("year")),
            cardNumber = clean(identity.opt("cardNumber")),
            grade = clean(certification.opt("grade")),
            gradingCompany = clean(certification.opt("company")),
            certSerialNumber = clean(certification.opt("serialNumber")),
            observations = observations,
            verificationSummary = providerSummary,
            notices = notices
        )
    }

    // Android JSONObject.optString() turns JSON null into the word "null".
    private fun clean(value: Any?): String {
        if (value == null || value == JSONObject.NULL) return ""
        val text = value.toString().trim()
        return if (text.isEmpty() || text.equals("null", ignoreCase = true)) "" else text
    }
}
