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

    @Test
    fun `optionalPositivePrice allows null but rejects non positive values`() {
        assertEquals(null, RequestValidation.optionalPositivePrice(null, "targetPrice"))
        assertEquals(120.0, RequestValidation.optionalPositivePrice(120.0, "targetPrice"))
        assertFailsWith<ValidationException> { RequestValidation.optionalPositivePrice(-1.0, "targetPrice") }
    }

    @Test
    fun `alertTravelWindow accepts a single future date or a valid range`() {
        val futureDate = java.time.LocalDate.now().plusMonths(4)
        val (from, to) = RequestValidation.alertTravelWindow(futureDate.toString(), null)
        assertEquals(futureDate, from)
        assertEquals(null, to)

        val futureEnd = futureDate.plusDays(5)
        val (rangeFrom, rangeTo) = RequestValidation.alertTravelWindow(futureDate.toString(), futureEnd.toString())
        assertEquals(futureDate, rangeFrom)
        assertEquals(futureEnd, rangeTo)
    }

    @Test
    fun `alertTravelWindow rejects past dates and inverted ranges`() {
        assertFailsWith<ValidationException> { RequestValidation.alertTravelWindow("2020-01-01", null) }
        val futureDate = java.time.LocalDate.now().plusMonths(4)
        assertFailsWith<ValidationException> {
            RequestValidation.alertTravelWindow(futureDate.toString(), futureDate.minusDays(1).toString())
        }
    }
}

