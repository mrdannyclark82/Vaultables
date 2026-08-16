package com.example.data.remote

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONException
import java.io.IOException
import java.util.concurrent.TimeUnit

data class VerificationCandidate(
    val source: String,
    val title: String,
    val confidence: String = "",
    val url: String = ""
)

data class CardVerificationResult(
    val candidates: List<VerificationCandidate>,
    val notices: List<String>
) {
    fun promptEvidence(): String = buildString {
        if (candidates.isNotEmpty()) {
            appendLine("Independent catalog evidence (not proof; reconcile it with visible card text):")
            candidates.forEach { candidate ->
                appendLine("- ${candidate.source}: ${candidate.title}${candidate.confidence.takeIf { it.isNotBlank() }?.let { " (confidence $it)" } ?: ""}")
            }
        }
        notices.forEach { appendLine("- Verification notice: $it") }
    }.trim()

    fun summary(): String = (candidates.map { "${it.source}: ${it.title}" } + notices).joinToString(" | ")
}

object CardVerificationService {
    private const val CARD_SIGHT_URL = "https://api.cardsight.ai/v1/identify/card"
    private const val GOOGLE_SEARCH_URL = "https://www.googleapis.com/customsearch/v1"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun verify(
        context: Context,
        frontImageUri: String,
        backImageUri: String?,
        titleHint: String,
        brandHint: String,
        yearHint: String
    ): CardVerificationResult = withContext(Dispatchers.IO) {
        val notices = mutableListOf<String>()
        val candidates = mutableListOf<VerificationCandidate>()
        val cardSightKey = BuildConfig.CARDSIGHT_API_KEY
        if (cardSightKey.isBlank() || cardSightKey.startsWith("your_")) {
            notices += "CardSight verification is unavailable until CARDSIGHT_API_KEY is configured."
        } else {
            try {
                identifyWithCardSight(context, frontImageUri, cardSightKey, "front")?.let(candidates::addAll)
                backImageUri?.let { identifyWithCardSight(context, it, cardSightKey, "back") }?.let(candidates::addAll)
            } catch (e: IOException) {
                notices += "CardSight verification failed: ${e.message}"
            } catch (e: JSONException) {
                notices += "CardSight returned invalid verification data."
            }
        }

        val query = candidates.firstOrNull()?.title
            ?: listOf(yearHint, brandHint, titleHint, "trading card").filter { it.isNotBlank() }.joinToString(" ")
        val googleKey = BuildConfig.GOOGLE_CUSTOM_SEARCH_API_KEY
        val searchEngineId = BuildConfig.GOOGLE_CUSTOM_SEARCH_ENGINE_ID
        if (googleKey.isBlank() || googleKey.startsWith("your_") || searchEngineId.isBlank() || searchEngineId.startsWith("your_")) {
            notices += "Google image verification is unavailable until GOOGLE_CUSTOM_SEARCH_API_KEY and GOOGLE_CUSTOM_SEARCH_ENGINE_ID are configured."
        } else if (query.isNotBlank()) {
            try {
                searchGoogleImages(query, googleKey, searchEngineId)?.let(candidates::addAll)
            } catch (e: IOException) {
                notices += "Google image verification failed: ${e.message}"
            } catch (e: JSONException) {
                notices += "Google returned invalid verification data."
            }
        }
        CardVerificationResult(candidates.distinctBy { "${it.source}:${it.title}:${it.url}" }, notices)
    }

    private fun identifyWithCardSight(
        context: Context,
        imageUri: String,
        apiKey: String,
        side: String
    ): List<VerificationCandidate>? {
        val bytes = CardImageProcessor.prepareForUpload(context, imageUri) ?: return null
        val request = Request.Builder()
            .url(CARD_SIGHT_URL)
            .header("X-API-Key", apiKey)
            .post(
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "$side-card.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
                    .build()
            )
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val detections = JSONObject(response.body?.string().orEmpty()).optJSONArray("detections") ?: return emptyList()
            return buildList {
                for (index in 0 until detections.length()) {
                    val detection = detections.optJSONObject(index) ?: continue
                    val card = detection.optJSONObject("card") ?: continue
                    val title = listOf(
                        card.optString("year"),
                        card.optString("manufacturer"),
                        card.optString("releaseName"),
                        card.optString("name"),
                        card.optString("number").takeIf { it.isNotBlank() }?.let { "#$it" }
                    ).filter { !it.isNullOrBlank() }.joinToString(" ")
                    if (title.isNotBlank()) add(VerificationCandidate("CardSight ($side)", title, detection.optString("confidence")))
                }
            }
        }
    }

    private fun searchGoogleImages(query: String, apiKey: String, searchEngineId: String): List<VerificationCandidate>? {
        val url = GOOGLE_SEARCH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("cx", searchEngineId)
            .addQueryParameter("q", query)
            .addQueryParameter("searchType", "image")
            .addQueryParameter("num", "5")
            .build()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val items = JSONObject(response.body?.string().orEmpty()).optJSONArray("items") ?: return emptyList()
            return buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val title = item.optString("title")
                    if (title.isNotBlank()) add(VerificationCandidate("Google image search", title, url = item.optString("link")))
                }
            }
        }
    }
}
