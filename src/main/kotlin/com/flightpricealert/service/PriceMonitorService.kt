package com.flightpricealert.service

import com.flightpricealert.amadeus.AmadeusClient
import com.flightpricealert.domain.FlightAlert
import com.flightpricealert.email.EmailService
import com.flightpricealert.repository.AlertRepository
import com.flightpricealert.repository.NotificationLogRepository
import com.flightpricealert.repository.PriceHistoryRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate

class PriceMonitorService(
    private val amadeusClient: AmadeusClient,
    private val emailService: EmailService
) {
    private val log = LoggerFactory.getLogger(PriceMonitorService::class.java)

    suspend fun checkAllActiveAlerts() {
        AlertRepository.listActive().forEach { alert ->
            try {
                checkAlert(alert)
            } catch (t: Throwable) {
                log.error("Failed checking alert id=${alert.id}", t)
            }
        }
    }

    suspend fun checkAlert(alert: FlightAlert): Double {
        val departureDate = LocalDate.now().plusMonths(1).withDayOfMonth(1).toString()
        val lowest = getLowestPrice(alert.origin, alert.destination, departureDate)
            ?: mockPrice(alert)

        if (alert.id != null) {
            PriceHistoryRepository.add(alert.id, lowest)
            maybeSendNotification(alert, lowest)
        }

        return lowest
    }

    suspend fun getLowestPrice(origin: String, destination: String, departureDate: String): Double? {
        return amadeusClient.findLowestPrice(origin, destination, departureDate)?.toDouble()
    }

    private fun maybeSendNotification(alert: FlightAlert, currentPrice: Double) {
        val alertId = alert.id ?: return
        if (currentPrice > alert.targetPrice) return

        val lastSent = NotificationLogRepository.lastSentPrice(alertId)
        if (lastSent != null && currentPrice >= lastSent) return

        val recipients = listOfNotNull(System.getenv("ALERT_RECIPIENT"))
        if (recipients.isNotEmpty()) {
            emailService.sendAlert(
                subject = "Flight alert ${alert.origin} -> ${alert.destination}",
                body = "New price found: $currentPrice (target=${alert.targetPrice})",
                recipients = recipients
            )
        }

        NotificationLogRepository.add(alertId = alertId, price = currentPrice, recipient = recipients.firstOrNull())
        log.info("Alert notified id={} price={}", alertId, currentPrice)
    }

    private fun mockPrice(alert: FlightAlert): Double {
        val seed = (alert.origin + alert.destination).sumOf { it.code }
        return 300.0 + (seed % 700)
    }
}



