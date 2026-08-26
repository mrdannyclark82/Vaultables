package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecureScanResultParserTest {

    @Test
    fun parsesMarketplaceSearchesFromSecureScanResponse() {
        val result = SecureScanResultParser.parse(
            """
            {
              "extracted": {
                "identity": {
                  "title": "2023 Topps Aaron Judge",
                  "brand": "Topps",
                  "set": "Chrome",
                  "year": "2023",
                  "cardNumber": "99"
                }
              },
              "marketplaceSearches": {
                "query": "2023 Topps Aaron Judge",
                "markets": [
                  {"name": "eBay sold", "url": "https://www.ebay.com/sch/i.html?_nkw=Aaron%20Judge"},
                  {"name": "Mercari", "url": "https://www.mercari.com/search/?keyword=Aaron%20Judge"},
                  {"name": "Invalid", "url": "javascript:alert(1)"}
                ]
              }
            }
            """.trimIndent()
        )

        assertEquals("2023 Topps Aaron Judge", result.title)
        assertEquals(listOf("eBay sold", "Mercari"), result.marketplaceSearches.map { it.name })
        assertTrue(result.marketplaceSearches.all { it.url.startsWith("https://") })
    }
}
