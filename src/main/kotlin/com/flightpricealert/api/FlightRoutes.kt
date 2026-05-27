package com.flightpricealert.api

import com.flightpricealert.repository.PriceHistoryRepository
import com.flightpricealert.service.AppServices
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class SearchPriceResponse(
    val origin: String,
    val destination: String,
    val departureDate: String,
    val lowestPrice: Double?,
    val source: String
)

fun Route.flightRoutes() {
    route("/flights") {
        get("/search") {
            call.respondApi {
                if (!SimpleRateLimiter.allow("search", limitPerMinute = AppServices.config().rateLimitPerMinute)) {
                    respond(HttpStatusCode.TooManyRequests, ApiErrorResponse("rate_limited", listOf("Too many requests, please slow down")))
                    return@respondApi
                }

                val origin = call.request.queryParameters["origin"]
                val destination = call.request.queryParameters["destination"]
                val departureDate = call.request.queryParameters["departureDate"]
                    ?: LocalDate.now().plusMonths(1).withDayOfMonth(1).toString()
                val endDate = call.request.queryParameters["endDate"]
                val month = call.request.queryParameters["month"]
                val airlines = call.request.queryParameters["airlines"]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }

                val normalizedOrigin = RequestValidation.iata(origin, "origin")
                val normalizedDestination = RequestValidation.iata(destination, "destination")
                val normalizedDepartureDate = RequestValidation.date(departureDate, "departureDate").toString()
                val normalizedMonth = month?.let { RequestValidation.month(it) }
                val normalizedRange = if (!endDate.isNullOrBlank()) RequestValidation.dateRange(departureDate, endDate) else null
                val normalizedAirlines = RequestValidation.airlineCodes(airlines)

                val monitor = AppServices.monitorService()
                val price = when {
                    normalizedMonth != null -> monitor.getLowestPriceByMonth(normalizedOrigin, normalizedDestination, normalizedMonth, normalizedAirlines)
                    normalizedRange != null -> monitor.getLowestPriceByDateRange(normalizedOrigin, normalizedDestination, normalizedRange.first.toString(), normalizedRange.second.toString(), normalizedAirlines)
                    else -> monitor.getLowestPrice(normalizedOrigin, normalizedDestination, normalizedDepartureDate)
                }
                respond(
                    SearchPriceResponse(
                        origin = normalizedOrigin,
                        destination = normalizedDestination,
                        departureDate = normalizedDepartureDate,
                        lowestPrice = price,
                        source = if (price == null) "mock-required-no-amadeus-creds" else "amadeus"
                    )
                )
            }
        }

        get("/price-history/{alertId}") {
            call.respondApi {
                val alertId = call.parameters["alertId"]?.toIntOrNull() ?: throw ValidationException(listOf("alertId must be an integer"))
                respond(PriceHistoryRepository.listByAlert(alertId))
            }
        }
    }
}

