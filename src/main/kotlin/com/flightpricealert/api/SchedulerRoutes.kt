package com.flightpricealert.api

import com.flightpricealert.scheduler.SchedulerService
import io.ktor.server.application.call
import io.ktor.server.routing.*
import io.ktor.server.response.*

fun Route.schedulerRoutes() {
    route("/scheduler") {
        get("/status") {
            call.respondText(SchedulerService.status())
        }

        post("/start") {
            SchedulerService.ensureRunning()
            call.respondText("scheduler started")
        }

        post("/stop") {
            SchedulerService.stop()
            call.respondText("scheduler stopped")
        }
    }
}

