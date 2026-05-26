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
            val origin = call.request.queryParameters["origin"]
            val destination = call.request.queryParameters["destination"]
            val departureDate = call.request.queryParameters["departureDate"]
                ?: LocalDate.now().plusMonths(1).withDayOfMonth(1).toString()

            if (origin.isNullOrBlank() || destination.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "origin and destination are required")
                return@get
            }

            val price = AppServices.monitorService().getLowestPrice(origin, destination, departureDate)
            call.respond(
                SearchPriceResponse(
                    origin = origin,
                    destination = destination,
                    departureDate = departureDate,
                    lowestPrice = price,
                    source = if (price == null) "mock-required-no-amadeus-creds" else "amadeus"
                )
            )
        }

        get("/price-history/{alertId}") {
            val alertId = call.parameters["alertId"]?.toIntOrNull()
            if (alertId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid alertId")
                return@get
            }
            call.respond(PriceHistoryRepository.listByAlert(alertId))
        }
    }
}

