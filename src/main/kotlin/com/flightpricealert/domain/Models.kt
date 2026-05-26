package com.flightpricealert.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class FlightAlert(
    val id: Int? = null,
    val origin: String,
    val destination: String,
    val targetPrice: Double,
    val airlines: List<String>? = null,
    val active: Boolean = true,
    val createdAt: LocalDateTime? = null
)

@Serializable
data class PriceHistory(
    val id: Int? = null,
    val alertId: Int,
    val price: Double,
    val checkedAt: LocalDateTime
)

@Serializable
data class NotificationLog(
    val id: Int? = null,
    val alertId: Int,
    val price: Double,
    val sentAt: LocalDateTime,
    val recipient: String? = null
)



