package com.flightpricealert.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class FlightAlert(
    val id: Int? = null,
    val origin: String,
    val destination: String,
    @Serializable(with = LocalDateSerializer::class)
    val departureDateFrom: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val departureDateTo: LocalDate? = null,
    val targetPrice: Double? = null,
    val airlines: List<String>? = null,
    val active: Boolean = true,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null
)

@Serializable
data class PriceHistory(
    val id: Int? = null,
    val alertId: Int,
    val price: Double,
    @Serializable(with = LocalDateTimeSerializer::class)
    val checkedAt: LocalDateTime
)

@Serializable
data class NotificationLog(
    val id: Int? = null,
    val alertId: Int,
    val price: Double,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sentAt: LocalDateTime,
    val recipient: String? = null
)



