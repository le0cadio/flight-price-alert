package com.flightpricealert.repository

import com.flightpricealert.domain.FlightAlert
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object AlertRepository {
    private val idSequence = AtomicInteger(1)
    private val alerts = ConcurrentHashMap<Int, FlightAlert>()

    fun create(origin: String, destination: String, targetPrice: Double, airlines: List<String>?): FlightAlert {
        val id = idSequence.getAndIncrement()
        val now = LocalDateTime.now()

        val alert = FlightAlert(
            id = id,
            origin = origin,
            destination = destination,
            targetPrice = targetPrice,
            airlines = airlines,
            active = true,
            createdAt = now
        )
        alerts[id] = alert
        return alert
    }

    fun listAll(): List<FlightAlert> = alerts.values.sortedBy { it.id }

    fun listActive(): List<FlightAlert> = alerts.values.filter { it.active }.sortedBy { it.id }

    fun findById(id: Int): FlightAlert? = alerts[id]

    fun update(id: Int, origin: String, destination: String, targetPrice: Double, airlines: List<String>?, active: Boolean): FlightAlert? {
        val current = alerts[id] ?: return null
        val updated = current.copy(
            origin = origin,
            destination = destination,
            targetPrice = targetPrice,
            airlines = airlines,
            active = active
        )
        alerts[id] = updated
        return updated
    }

    fun setActive(id: Int, active: Boolean): FlightAlert? {
        val current = alerts[id] ?: return null
        val updated = current.copy(active = active)
        alerts[id] = updated
        return updated
    }

    fun delete(id: Int): Boolean = alerts.remove(id) != null
}






