package com.flightpricealert

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.callloging.*
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8080) {
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
        }
    }.start(wait = true)
}
