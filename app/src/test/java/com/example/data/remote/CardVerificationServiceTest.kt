package com.example.data.remote

import org.junit.Assert.assertTrue
import org.junit.Test

class CardVerificationServiceTest {
    @Test
    fun `prompt evidence identifies every independent source`() {
        val result = CardVerificationResult(
            candidates = listOf(
                VerificationCandidate("CardSight (front)", "1996 Topps Kobe Bryant #138", "High"),
                VerificationCandidate("Google image search", "1996 Topps Kobe Bryant #138")
            ),
            notices = emptyList()
        )

        val evidence = result.promptEvidence()

        assertTrue(evidence.contains("CardSight (front)"))
        assertTrue(evidence.contains("Google image search"))
        assertTrue(evidence.contains("1996 Topps Kobe Bryant #138"))
    }
}
