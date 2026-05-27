package com.flightpricealert.service

import com.flightpricealert.amadeus.FlightPriceProvider
import com.flightpricealert.domain.FlightAlert
import com.flightpricealert.email.EmailNotifier
import com.flightpricealert.email.EmailSendResult
import com.flightpricealert.repository.AlertRepository
import com.flightpricealert.repository.NotificationLogRepository
import com.flightpricealert.repository.PriceHistoryRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

class PriceMonitorService(
    private val priceProvider: FlightPriceProvider,
    private val emailService: EmailNotifier,
    private val alertRecipients: List<String>
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
        return priceProvider.findLowestPrice(origin, destination, departureDate)?.toDouble()
    }

    suspend fun getLowestPriceByMonth(origin: String, destination: String, month: YearMonth, airlines: List<String>? = null): Double? {
        return priceProvider.findLowestPriceByMonth(origin, destination, month, airlines)?.toDouble()
    }

    suspend fun getLowestPriceByDateRange(
        origin: String,
        destination: String,
        startDate: String,
        endDate: String,
        airlines: List<String>? = null
    ): Double? {
        return priceProvider.findLowestPriceByDateRange(origin, destination, startDate, endDate, airlines)?.toDouble()
    }

    private fun maybeSendNotification(alert: FlightAlert, currentPrice: Double) {
        val alertId = alert.id ?: return
        if (currentPrice > alert.targetPrice) return

        val lastSent = NotificationLogRepository.lastSentPrice(alertId)
        if (lastSent != null && currentPrice >= lastSent) return

        val recipients = alertRecipients.distinct()
        if (recipients.isNotEmpty()) {
            val htmlBody = com.flightpricealert.email.EmailTemplate.alertHtmlBody(
                origin = alert.origin,
                destination = alert.destination,
                currentPrice = currentPrice,
                targetPrice = alert.targetPrice
            )
            when (val result = emailService.sendAlert(
                subject = "✈️ Alerta de preço: ${alert.origin} → ${alert.destination}",
                body = htmlBody,
                recipients = recipients
            )) {
                is EmailSendResult.Sent -> log.info("Email dispatched to {} for alert {}", result.recipients, alertId)
                is EmailSendResult.Skipped -> log.warn("Email skipped for alert {}: {}", alertId, result.reason)
                is EmailSendResult.Failed -> log.warn("Email failed for alert {}: {}", alertId, result.reason)
            }
        }

        NotificationLogRepository.add(alertId = alertId, price = currentPrice, recipient = recipients.firstOrNull())
        log.info("Alert notified id={} price={}", alertId, currentPrice)
    }

    private fun mockPrice(alert: FlightAlert): Double {
        val seed = (alert.origin + alert.destination).sumOf { it.code }
        return 300.0 + (seed % 700)
    }
}



