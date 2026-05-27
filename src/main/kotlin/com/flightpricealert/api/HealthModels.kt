package com.flightpricealert.api

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val environment: String,
    val database: Boolean,
    val amadeusConfigured: Boolean,
    val smtpConfigured: Boolean,
    val schedulerStatus: String,
    val uptimeSeconds: Long,
    val timestamp: String
)

