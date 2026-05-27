package com.flightpricealert

import com.flightpricealert.api.HealthResponse
import com.flightpricealert.config.Config
import com.flightpricealert.repository.DatabaseFactory
import com.flightpricealert.service.AppServices
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.flightpricealert.api.alertsRoutes
import com.flightpricealert.api.emailRoutes
import com.flightpricealert.api.flightRoutes
import com.flightpricealert.api.schedulerRoutes
import io.ktor.server.plugins.callloging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import org.slf4j.event.Level

fun main() {
    val cfg = Config.load()

    embeddedServer(Netty, port = cfg.port) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val cfg = Config.load()
    val startedAt = Instant.now()

    DatabaseFactory.init(cfg)
    AppServices.init(cfg)

    install(CallLogging) {
        level = Level.valueOf(cfg.logLevel.uppercase())
    }

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/health") {
            call.respond(
                HealthResponse(
                    status = if (DatabaseFactory.ping()) "UP" else "DEGRADED",
                    environment = cfg.appEnvironment.name,
                    database = DatabaseFactory.ping(),
                    amadeusConfigured = !cfg.amadeusClientId.isNullOrBlank() && !cfg.amadeusClientSecret.isNullOrBlank(),
                    smtpConfigured = !cfg.smtpHost.isNullOrBlank(),
                    schedulerStatus = com.flightpricealert.scheduler.SchedulerService.status(),
                    uptimeSeconds = java.time.Duration.between(startedAt, Instant.now()).seconds,
                    timestamp = Instant.now().toString()
                )
            )
        }

        get("/") {
            call.respondText("Flight Price Alert is running ✈️")
        }
        route("/api") {
            alertsRoutes()
            flightRoutes()
            emailRoutes()
            schedulerRoutes()
        }

        get("/config") {
            call.respond(
                mapOf(
                    "environment" to cfg.appEnvironment.name,
                    "port" to cfg.port,
                    "schedulerIntervalHours" to cfg.schedulerIntervalHours,
                    "rateLimitPerMinute" to cfg.rateLimitPerMinute,
                    "requestTimeoutMillis" to cfg.requestTimeoutMillis,
                    "requestRetryCount" to cfg.requestRetryCount,
                    "amadeusConfigured" to (!cfg.amadeusClientId.isNullOrBlank() && !cfg.amadeusClientSecret.isNullOrBlank()),
                    "smtpConfigured" to (!cfg.smtpHost.isNullOrBlank()),
                    "database" to cfg.dbUrl
                )
            )
        }
    }

    val scope = CoroutineScope(SupervisorJob())
    scope.launch {
        com.flightpricealert.scheduler.SchedulerService.start(
            scope = scope,
            intervalHours = cfg.schedulerIntervalHours,
            worker = { AppServices.monitorService().checkAllActiveAlerts() }
        )
    }
}
