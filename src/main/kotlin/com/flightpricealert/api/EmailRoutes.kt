package com.flightpricealert.api

import com.flightpricealert.service.AppServices
import com.flightpricealert.email.EmailSendResult
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
            call.respondApi {
                val clientKey = "email:${call.request.local.remoteHost}"
                if (!SimpleRateLimiter.allow(clientKey, limitPerMinute = AppServices.config().rateLimitPerMinute)) {
                    respond(HttpStatusCode.TooManyRequests, ApiErrorResponse("rate_limited", listOf("Too many requests, please slow down")))
                    return@respondApi
                }

                val req = receive<EmailTestRequest>()
                val recipients = RequestValidation.recipients(req.recipients)
                val result = AppServices.emailService().sendAlert(req.subject.trim(), req.body.trim(), recipients)

                when (result) {
                    is EmailSendResult.Sent -> respond(HttpStatusCode.OK, mapOf("status" to "sent", "recipients" to result.recipients))
                    is EmailSendResult.Skipped -> respond(HttpStatusCode.Accepted, ApiErrorResponse("email_skipped", listOf(result.reason)))
                    is EmailSendResult.Failed -> respond(HttpStatusCode.InternalServerError, ApiErrorResponse("email_failed", listOf(result.reason)))
                }
            }
        }
    }
}

