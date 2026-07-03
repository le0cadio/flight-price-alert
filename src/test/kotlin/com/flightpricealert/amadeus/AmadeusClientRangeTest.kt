package com.flightpricealert.amadeus

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AmadeusClientRangeTest {

    @Test
    fun `buildDateRange returns all dates in range`() {
        val amadeus = AmadeusClient(HttpClient(CIO), null, null)
        val dates = amadeus.buildDateRange("2026-06-01", "2026-06-03")

        assertEquals(listOf("2026-06-01", "2026-06-02", "2026-06-03"), dates?.map { it.toString() })
    }

    @Test
    fun `buildMonthDates returns complete month`() {
        val amadeus = AmadeusClient(HttpClient(CIO), null, null)
        val dates = amadeus.buildMonthDates(YearMonth.of(2026, 2))

        assertEquals(28, dates.size)
        assertEquals("2026-02-01", dates.first().toString())
        assertEquals("2026-02-28", dates.last().toString())
    }

    @Test
    fun `sampleDates caps a wide window to protect the free quota`() {
        val amadeus = AmadeusClient(HttpClient(CIO), null, null)
        val dates = amadeus.buildMonthDates(YearMonth.of(2026, 11))

        val sampled = amadeus.sampleDates(dates, maxDates = 10)

        assertEquals(10, sampled.size)
        assertEquals(dates.first(), sampled.first())
        assertTrue(sampled.all { it in dates })
    }

    @Test
    fun `sampleDates returns all dates when under the cap`() {
        val amadeus = AmadeusClient(HttpClient(CIO), null, null)
        val dates = amadeus.buildDateRange("2026-06-01", "2026-06-03")!!

        assertEquals(dates, amadeus.sampleDates(dates, maxDates = 10))
    }

    @Test
    fun `buildDateRange returns null for invalid values`() {
        val amadeus = AmadeusClient(HttpClient(CIO), null, null)

        assertNull(amadeus.buildDateRange("2026-06-03", "2026-06-01"))
        assertNull(amadeus.buildDateRange("invalid-date", "2026-06-01"))
    }

    @Test
    fun `extractLowestPriceFromOffers applies airline filter`() {
        val amadeus = AmadeusClient(HttpClient(CIO), null, null)
        val payload = """
            {
              "data": [
                {
                  "price": { "grandTotal": "500.00" },
                  "itineraries": [
                    { "segments": [ { "operating": { "carrierCode": "AD" } } ] }
                  ]
                },
                {
                  "price": { "grandTotal": "650.00" },
                  "itineraries": [
                    { "segments": [ { "operating": { "carrierCode": "LA" } } ] }
                  ]
                }
              ]
            }
        """.trimIndent()

        val minForLA = amadeus.extractLowestPriceFromOffers(payload, listOf("LA"))
        val minForAD = amadeus.extractLowestPriceFromOffers(payload, listOf("AD"))
        val minWithoutFilter = amadeus.extractLowestPriceFromOffers(payload, null)

        assertEquals("650.00", minForLA?.setScale(2)?.toPlainString())
        assertEquals("500.00", minForAD?.setScale(2)?.toPlainString())
        assertEquals("500.00", minWithoutFilter?.setScale(2)?.toPlainString())
    }
}


