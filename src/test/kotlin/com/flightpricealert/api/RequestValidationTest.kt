package com.flightpricealert.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RequestValidationTest {
    @Test
    fun `validates IATA codes and airline codes`() {
        assertEquals("GRU", RequestValidation.iata("gru", "origin"))
        assertEquals(listOf("LA", "AF"), RequestValidation.airlineCodes(listOf(" la ", "AF")))
    }

    @Test
    fun `rejects invalid IATA and airlines`() {
        assertFailsWith<ValidationException> { RequestValidation.iata("GR", "origin") }
        assertFailsWith<ValidationException> { RequestValidation.airlineCodes(listOf("LONGCODE")) }
    }

    @Test
    fun `validates dates month ranges recipients and price`() {
        assertEquals(2026, RequestValidation.month("2026-06").year)
        assertEquals(1, RequestValidation.date("2026-06-01", "departureDate").dayOfMonth)
        assertEquals("2026-06-01" to "2026-06-03", RequestValidation.dateRange("2026-06-01", "2026-06-03").let { it.first.toString() to it.second.toString() })
        assertEquals(listOf("a@b.com"), RequestValidation.recipients(listOf(" a@b.com ")))
        assertEquals(120.0, RequestValidation.positivePrice(120.0, "targetPrice"))
    }

    @Test
    fun `rejects invalid dates and recipients`() {
        assertFailsWith<ValidationException> { RequestValidation.month("2026/06") }
        assertFailsWith<ValidationException> { RequestValidation.date("2026-06-31", "departureDate") }
        assertFailsWith<ValidationException> { RequestValidation.dateRange("2026-06-03", "2026-06-01") }
        assertFailsWith<ValidationException> { RequestValidation.recipients(emptyList()) }
        assertFailsWith<ValidationException> { RequestValidation.positivePrice(0.0, "targetPrice") }
    }
}

