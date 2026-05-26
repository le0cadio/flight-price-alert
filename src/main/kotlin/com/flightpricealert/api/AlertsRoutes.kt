package com.flightpricealert.api

import com.flightpricealert.domain.FlightAlert
import com.flightpricealert.repository.AlertRepository
import io.ktor.server.application.*
import io.ktor.http.HttpStatusCode
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
            val req = call.receive<CreateAlertRequest>()
            if (req.origin.isBlank() || req.destination.isBlank() || req.targetPrice <= 0.0) {
                call.respond(HttpStatusCode.BadRequest, "Invalid alert payload")
                return@post
            }
            val created: FlightAlert = AlertRepository.create(
                origin = req.origin,
                destination = req.destination,
                targetPrice = req.targetPrice,
                airlines = req.airlines
            )
            call.respond(HttpStatusCode.Created, created)
        }

        get {
            val list = AlertRepository.listAll()
            call.respond(list)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@get
            }
            val alert = AlertRepository.findById(id)
            if (alert == null) call.respond(HttpStatusCode.NotFound, "Alert not found") else call.respond(alert)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@put
            }
            val req = call.receive<UpdateAlertRequest>()
            val updated = AlertRepository.update(
                id = id,
                origin = req.origin,
                destination = req.destination,
                targetPrice = req.targetPrice,
                airlines = req.airlines,
                active = req.active
            )
            if (updated == null) call.respond(HttpStatusCode.NotFound, "Alert not found") else call.respond(updated)
        }

        post("/{id}/toggle") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@post
            }
            val req = call.receive<ToggleAlertRequest>()
            val updated = AlertRepository.setActive(id, req.active)
            if (updated == null) call.respond(HttpStatusCode.NotFound, "Alert not found") else call.respond(updated)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@delete
            }
            val deleted = AlertRepository.delete(id)
            if (!deleted) call.respond(HttpStatusCode.NotFound, "Alert not found") else call.respond(HttpStatusCode.NoContent)
        }
    }
}



