@file:Suppress("unused", "UNUSED_PARAMETER")

package com.flightpricealert.amadeus

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

@Serializable
data class AccessTokenResponse(val access_token: String, val token_type: String, val expires_in: Int)

interface FlightPriceProvider {
    suspend fun findLowestPrice(origin: String, destination: String, departureDate: String): BigDecimal?
    suspend fun findLowestPriceByMonth(origin: String, destination: String, month: YearMonth, airlines: List<String>? = null): BigDecimal?
    suspend fun findLowestPriceByDateRange(origin: String, destination: String, startDate: String, endDate: String, airlines: List<String>? = null): BigDecimal?
}

class AmadeusClient(
    private val client: HttpClient,
    private val clientId: String?,
    private val clientSecret: String?,
    private val requestRetryCount: Int = 1
) : FlightPriceProvider {
    private var token: AccessTokenResponse? = null
    private var tokenValidUntilEpochSec: Long = 0

    suspend fun obtainToken(): AccessTokenResponse? {
        val now = Instant.now().epochSecond
        if (token != null && now < tokenValidUntilEpochSec) return token
        if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank()) return null

        val responseText: String = withRetries {
            client.post("https://test.api.amadeus.com/v1/security/oauth2/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("grant_type=client_credentials&client_id=${clientId}&client_secret=${clientSecret}")
            }.bodyAsText()
        }

        val parsed = Json.decodeFromString<AccessTokenResponse>(responseText)
        token = parsed
        tokenValidUntilEpochSec = now + parsed.expires_in - 30
        return parsed
    }

    override suspend fun findLowestPrice(origin: String, destination: String, departureDate: String): BigDecimal? {
        return findLowestPrice(origin, destination, departureDate, null)
    }

    @Suppress("unused")
    override suspend fun findLowestPriceByMonth(
        origin: String,
        destination: String,
        month: YearMonth,
        airlines: List<String>?
    ): BigDecimal? {
        return findLowestPriceAcrossDates(origin, destination, buildMonthDates(month), airlines)
    }

    @Suppress("unused")
    override suspend fun findLowestPriceByDateRange(
        origin: String,
        destination: String,
        startDate: String,
        endDate: String,
        airlines: List<String>?
    ): BigDecimal? {
        if (endDate.isBlank()) return null

        val dates = buildDateRange(startDate, endDate) ?: return null
        return findLowestPriceAcrossDates(origin, destination, dates, airlines)
    }

    private suspend fun findLowestPrice(
        origin: String,
        destination: String,
        departureDate: String,
        airlines: List<String>?
    ): BigDecimal? {
        val t = obtainToken() ?: return null

        return try {
            val resultText: String = withRetries {
                client.get("https://test.api.amadeus.com/v2/shopping/flight-offers") {
                    header(HttpHeaders.Authorization, "Bearer ${t.access_token}")
                    url {
                        parameters.append("originLocationCode", origin)
                        parameters.append("destinationLocationCode", destination)
                        parameters.append("departureDate", departureDate)
                        parameters.append("adults", "1")
                        parameters.append("max", "250")
                    }
                }.bodyAsText()
            }

            extractLowestPriceFromOffers(resultText, airlines)
        } catch (e: Exception) {
            println(e.message)
            null
        }
    }

    private suspend fun findLowestPriceAcrossDates(
        origin: String,
        destination: String,
        dates: List<LocalDate>,
        airlines: List<String>?
    ): BigDecimal? {
        var minPrice: BigDecimal? = null

        for (date in dates) {
            val price = findLowestPrice(origin, destination, date.toString(), airlines) ?: continue
            minPrice = if (minPrice == null || price < minPrice) price else minPrice
        }

        return minPrice
    }

    internal fun buildMonthDates(month: YearMonth): List<LocalDate> {
        return datesInRange(month.atDay(1), month.atEndOfMonth())
    }

    internal fun buildDateRange(startDate: String, endDate: String): List<LocalDate>? {
        val start = parseDateOrNull(startDate) ?: return null
        val end = parseDateOrNull(endDate) ?: return null
        if (end.isBefore(start)) return null
        return datesInRange(start, end)
    }

    private fun parseDateOrNull(value: String): LocalDate? {
        return try {
            LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun datesInRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start

        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(1)
        }

        return dates
    }

    internal fun extractLowestPriceFromOffers(payload: String, airlineFilter: List<String>? = null): BigDecimal? {
        return try {
            val root = Json.parseToJsonElement(payload).jsonObject
            val data = root["data"]?.jsonArray ?: return null
            var min: BigDecimal? = null

            for (item in data) {
                if (airlineFilter != null && airlineFilter.isNotEmpty()) {
                    val itineraries = item.jsonObject["itineraries"]?.jsonArray
                    val matchesAirline = itineraries?.any { leg ->
                        leg.jsonObject["segments"]?.jsonArray?.any { segment ->
                            val carrierCode = segment.jsonObject["operating"]
                                ?.jsonObject?.get("carrierCode")
                                ?.jsonPrimitive?.contentOrNull
                                ?: segment.jsonObject["validatingAirlineCodes"]
                                    ?.jsonArray?.firstOrNull()
                                    ?.jsonPrimitive?.contentOrNull
                            carrierCode in airlineFilter
                        } ?: false
                    } ?: false
                    if (!matchesAirline) continue
                }

                val grandTotal = item.jsonObject["price"]
                    ?.jsonObject
                    ?.get("grandTotal")
                    ?.jsonPrimitive
                    ?.contentOrNull
                val price = grandTotal?.toBigDecimalOrNull() ?: continue
                min = if (min == null || price < min) price else min
            }

            min
        } catch (e: Exception) {
            println(e.message)
            null
        }
    }

    private suspend fun <T> withRetries(block: suspend () -> T): T {
        var lastError: Throwable? = null
        repeat(requestRetryCount.coerceAtLeast(1)) { attempt ->
            try {
                return block()
            } catch (t: Throwable) {
                lastError = t
                if (attempt < requestRetryCount.coerceAtLeast(1) - 1) {
                    delay(250L)
                }
            }
        }
        throw lastError ?: IllegalStateException("Unknown Amadeus request failure")
    }
}

