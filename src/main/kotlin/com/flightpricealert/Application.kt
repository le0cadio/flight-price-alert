package com.flightpricealert

import com.flightpricealert.config.Config
import com.flightpricealert.repository.DatabaseFactory
import com.flightpricealert.service.AppServices
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
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
import org.slf4j.event.Level

fun main() {
    val cfg = Config.load()

    embeddedServer(Netty, port = cfg.port) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val cfg = Config.load()

    DatabaseFactory.init(cfg)
    AppServices.init(cfg)

    install(CallLogging) {
        level = Level.INFO
    }


    routing {
        get("/health") {
            call.respondText("OK")
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
            call.respondText("Port=${cfg.port}, AmadeusClientIdSet=${!cfg.amadeusClientId.isNullOrBlank()}")
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
