package com.flightpricealert.api

import com.flightpricealert.service.AppServices
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class EmailTestRequest(
    val recipients: List<String>,
    val subject: String = "Flight Price Alert - Test",
    val body: String = "SMTP test successful"
)

fun Route.emailRoutes() {
    route("/email") {
        post("/test") {
            val req = call.receive<EmailTestRequest>()
            if (req.recipients.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "recipients cannot be empty")
                return@post
            }
            AppServices.emailService().sendAlert(req.subject, req.body, req.recipients)
            call.respond(HttpStatusCode.OK, "email test processed")
        }
    }
}

