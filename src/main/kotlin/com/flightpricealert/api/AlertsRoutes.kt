package com.flightpricealert.api

import com.flightpricealert.domain.FlightAlert
import com.flightpricealert.repository.AlertRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateAlertRequest(
    val origin: String,
    val destination: String,
    val targetPrice: Double,
    val airlines: List<String>? = null
)

@Serializable
data class UpdateAlertRequest(
    val origin: String,
    val destination: String,
    val targetPrice: Double,
    val airlines: List<String>? = null,
    val active: Boolean = true
)

@Serializable
data class ToggleAlertRequest(val active: Boolean)

fun Route.alertsRoutes() {
    route("/flights/alerts") {
        post {
            call.respondApi {
                val req = receive<CreateAlertRequest>()
                val origin = RequestValidation.iata(req.origin, "origin")
                val destination = RequestValidation.iata(req.destination, "destination")
                val targetPrice = RequestValidation.positivePrice(req.targetPrice, "targetPrice")
                val airlines = RequestValidation.airlineCodes(req.airlines)

                val created: FlightAlert = AlertRepository.create(
                    origin = origin,
                    destination = destination,
                    targetPrice = targetPrice,
                    airlines = airlines
                )
                respond(HttpStatusCode.Created, created)
            }
        }

        get {
            call.respondApi {
                respond(AlertRepository.listAll())
            }
        }

        get("/{id}") {
            call.respondApi {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw ValidationException(listOf("id must be an integer"))
                val alert = AlertRepository.findById(id) ?: throw NotFoundException("Alert not found")
                respond(alert)
            }
        }

        put("/{id}") {
            call.respondApi {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw ValidationException(listOf("id must be an integer"))
                val req = receive<UpdateAlertRequest>()
                val origin = RequestValidation.iata(req.origin, "origin")
                val destination = RequestValidation.iata(req.destination, "destination")
                val targetPrice = RequestValidation.positivePrice(req.targetPrice, "targetPrice")
                val airlines = RequestValidation.airlineCodes(req.airlines)

                val updated = AlertRepository.update(
                    id = id,
                    origin = origin,
                    destination = destination,
                    targetPrice = targetPrice,
                    airlines = airlines,
                    active = req.active
                ) ?: throw NotFoundException("Alert not found")
                respond(updated)
            }
        }

        post("/{id}/toggle") {
            call.respondApi {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw ValidationException(listOf("id must be an integer"))
                val req = receive<ToggleAlertRequest>()
                val updated = AlertRepository.setActive(id, req.active) ?: throw NotFoundException("Alert not found")
                respond(updated)
            }
        }

        delete("/{id}") {
            call.respondApi {
                val id = call.parameters["id"]?.toIntOrNull() ?: throw ValidationException(listOf("id must be an integer"))
                val deleted = AlertRepository.delete(id)
                if (!deleted) throw NotFoundException("Alert not found")
                respond(HttpStatusCode.NoContent)
            }
        }
    }
}



