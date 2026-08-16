package com.example

import com.example.data.remote.GeminiService
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiServiceTest {

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
    }

    @Test
    fun testAnalyzeAndAppraise() = runTest {
        println("=== STARTING GEMINI SERVICE TEST ===")
        try {
            val result = GeminiService.analyzeAndAppraise(
                context = ApplicationProvider.getApplicationContext(),
                title = "1996 Topps Kobe Bryant",
                category = "Trading Cards",
                notes = "PSA 9 rookie card",
                brand = "Topps",
                year = "1996"
            )
            println("=== RESULT ===\n$result")
            assertNotNull(result)
        } catch (e: Exception) {
            println("=== EXCEPTION ===")
            e.printStackTrace()
            throw e
        }
    }
}
