package com.flightpricealert.amadeus

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.Instant

@Serializable
data class AccessTokenResponse(val access_token: String, val token_type: String, val expires_in: Int)

class AmadeusClient(private val client: HttpClient, private val clientId: String?, private val clientSecret: String?) {
    private var token: AccessTokenResponse? = null
    private var tokenValidUntilEpochSec: Long = 0

    suspend fun obtainToken(): AccessTokenResponse? {
        val now = Instant.now().epochSecond
        if (token != null && now < tokenValidUntilEpochSec) return token
        if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank()) return null

        val responseText: String = client.post("https://test.api.amadeus.com/v1/security/oauth2/token") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("grant_type=client_credentials&client_id=${clientId}&client_secret=${clientSecret}")
        }.bodyAsText()

        val parsed = Json.decodeFromString<AccessTokenResponse>(responseText)
        token = parsed
        tokenValidUntilEpochSec = now + parsed.expires_in - 30
        return parsed
    }

    suspend fun findLowestPrice(origin: String, destination: String, departureDate: String): BigDecimal? {
        val t = obtainToken() ?: return null

        val resultText: String = client.get("https://test.api.amadeus.com/v2/shopping/flight-offers") {
            header(HttpHeaders.Authorization, "Bearer ${t.access_token}")
            url {
                parameters.append("originLocationCode", origin)
                parameters.append("destinationLocationCode", destination)
                parameters.append("departureDate", departureDate)
                parameters.append("adults", "1")
                parameters.append("max", "20")
            }
        }.bodyAsText()

        return extractLowestPriceFromOffers(resultText)
    }

    internal fun extractLowestPriceFromOffers(payload: String): BigDecimal? {
        val root = Json.parseToJsonElement(payload).jsonObject
        val data = root["data"]?.jsonArray ?: return null
        var min: BigDecimal? = null

        for (item in data) {
            val grandTotal = item.jsonObject["price"]
                ?.jsonObject
                ?.get("grandTotal")
                ?.jsonPrimitive
                ?.contentOrNull
            val price = grandTotal?.toBigDecimalOrNull() ?: continue
            min = if (min == null || price < min) price else min
        }

        return min
    }
}

