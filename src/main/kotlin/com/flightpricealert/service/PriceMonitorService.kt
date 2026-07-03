package com.flightpricealert.service

import com.flightpricealert.amadeus.FlightPriceProvider
import com.flightpricealert.domain.FlightAlert
import com.flightpricealert.domain.PriceStats
import com.flightpricealert.domain.Recommendation
import com.flightpricealert.email.EmailNotifier
import com.flightpricealert.email.EmailSendResult
import com.flightpricealert.email.EmailTemplate
import com.flightpricealert.repository.AlertRepository
import com.flightpricealert.repository.NotificationLogRepository
import com.flightpricealert.repository.PriceHistoryRepository
import org.slf4j.LoggerFactory
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

    /**
     * Returns the lowest price found for the alert's own travel window, or null if the
     * provider couldn't produce one (no credentials, no offers, request failure). A null
     * result is never invented — the cycle is simply skipped.
     */
    suspend fun checkAlert(alert: FlightAlert): Double? {
        val price = fetchPrice(alert)
        if (price == null) {
            log.warn(
                "No price available for alert id={} route={}->{} window={}..{}; skipping this cycle",
                alert.id, alert.origin, alert.destination, alert.departureDateFrom, alert.departureDateTo
            )
            return null
        }

        val alertId = alert.id
        if (alertId != null) {
            val statsBeforeThisCheck = PriceHistoryRepository.stats(alertId)
            val previousPrice = PriceHistoryRepository.lastPrice(alertId)
            PriceHistoryRepository.add(alertId, price)
            maybeSendNotification(alert, price, statsBeforeThisCheck, previousPrice)
        }

        return price
    }

    private suspend fun fetchPrice(alert: FlightAlert): Double? {
        val rangeEnd = alert.departureDateTo
        return if (rangeEnd != null && rangeEnd != alert.departureDateFrom) {
            priceProvider.findLowestPriceByDateRange(
                origin = alert.origin,
                destination = alert.destination,
                startDate = alert.departureDateFrom.toString(),
                endDate = rangeEnd.toString(),
                airlines = alert.airlines
            )?.toDouble()
        } else {
            priceProvider.findLowestPrice(
                origin = alert.origin,
                destination = alert.destination,
                departureDate = alert.departureDateFrom.toString()
            )?.toDouble()
        }
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

    private fun maybeSendNotification(
        alert: FlightAlert,
        currentPrice: Double,
        statsBeforeThisCheck: PriceStats,
        previousPrice: Double?
    ) {
        val alertId = alert.id ?: return

        val recommendation = PriceRecommendationEngine.evaluate(
            currentPrice = currentPrice,
            stats = statsBeforeThisCheck,
            previousPrice = previousPrice,
            targetPriceCeiling = alert.targetPrice
        )
        if (recommendation == Recommendation.NONE) return

        val lastSent = NotificationLogRepository.lastSentPrice(alertId)
        if (lastSent != null && currentPrice >= lastSent) return

        val recipients = alertRecipients.distinct()
        if (recipients.isNotEmpty()) {
            val htmlBody = EmailTemplate.alertHtmlBody(
                origin = alert.origin,
                destination = alert.destination,
                currentPrice = currentPrice,
                recommendation = recommendation,
                stats = statsBeforeThisCheck,
                departureDateFrom = alert.departureDateFrom,
                departureDateTo = alert.departureDateTo,
                targetPrice = alert.targetPrice
            )
            when (val result = emailService.sendAlert(
                subject = "${recommendation.emoji} ${alert.origin} → ${alert.destination}: ${recommendation.title}",
                body = htmlBody,
                recipients = recipients
            )) {
                is EmailSendResult.Sent -> log.info("Email dispatched to {} for alert {}", result.recipients, alertId)
                is EmailSendResult.Skipped -> log.warn("Email skipped for alert {}: {}", alertId, result.reason)
                is EmailSendResult.Failed -> log.warn("Email failed for alert {}: {}", alertId, result.reason)
            }
        }

        NotificationLogRepository.add(alertId = alertId, price = currentPrice, recipient = recipients.firstOrNull())
        log.info("Alert notified id={} price={} recommendation={}", alertId, currentPrice, recommendation)
    }
}
