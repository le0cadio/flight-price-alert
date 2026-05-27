package com.flightpricealert.service

import com.flightpricealert.amadeus.FlightPriceProvider
import com.flightpricealert.config.AppConfig
import com.flightpricealert.config.AppEnvironment
import com.flightpricealert.email.EmailNotifier
import com.flightpricealert.email.EmailSendResult
import com.flightpricealert.repository.AlertRepository
import com.flightpricealert.repository.DatabaseFactory
import com.flightpricealert.repository.NotificationLogRepository
import com.flightpricealert.repository.PriceHistoryRepository
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PriceMonitorServiceTest {
    private val testConfig = AppConfig(
        appEnvironment = AppEnvironment.LOCAL,
        port = 8080,
        amadeusClientId = null,
        amadeusClientSecret = null,
        alertRecipients = listOf("user@example.com"),
        smtpHost = null,
        smtpPort = 587,
        smtpUser = null,
        smtpPassword = null,
        dbUrl = "jdbc:h2:mem:monitor-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        dbUser = "sa",
        dbPassword = "",
        schedulerIntervalHours = 6L,
        logLevel = "DEBUG",
        requestTimeoutMillis = 5000L,
        requestRetryCount = 1,
        rateLimitPerMinute = 120
    )

    @BeforeTest
    fun setup() {
        DatabaseFactory.init(testConfig)
        DatabaseFactory.reset()
    }

    @Test
    fun `sends one notification for duplicate prices and sends again when price drops further`() = runBlocking {
        val alert = AlertRepository.create("GRU", "BKK", 200.0, null)
        val provider = SequentialPriceProvider(listOf(190.0, 190.0, 180.0))
        val notifier = RecordingNotifier()
        val service = PriceMonitorService(provider, notifier, listOf("user@example.com"))

        val first = service.checkAlert(alert)
        val second = service.checkAlert(alert)
        val third = service.checkAlert(alert)

        assertEquals(190.0, first)
        assertEquals(190.0, second)
        assertEquals(180.0, third)
        assertEquals(2, notifier.sentCount)
        assertEquals(180.0, NotificationLogRepository.lastSentPrice(requireNotNull(alert.id)))
    }

    @Test
    fun `falls back to mock price when api fails`() = runBlocking {
        val alert = AlertRepository.create("SAO", "MIA", 1000.0, null)
        val provider = FailingPriceProvider()
        val notifier = RecordingNotifier()
        val service = PriceMonitorService(provider, notifier, listOf("user@example.com"))

        val price = service.checkAlert(alert)

        assertTrue(price > 0.0)
        assertEquals(1, PriceHistoryRepository.listByAlert(requireNotNull(alert.id)).size)
        assertEquals(1, notifier.sentCount)
    }

    @Test
    fun `returns explicit month and range prices from provider`() = runBlocking {
        val provider = object : FlightPriceProvider {
            override suspend fun findLowestPrice(origin: String, destination: String, departureDate: String): BigDecimal? = nullablePrice(300.0)
            override suspend fun findLowestPriceByMonth(origin: String, destination: String, month: YearMonth, airlines: List<String>?): BigDecimal? = nullablePrice(250.0)
            override suspend fun findLowestPriceByDateRange(origin: String, destination: String, startDate: String, endDate: String, airlines: List<String>?): BigDecimal? = nullablePrice(240.0)
        }
        val notifier = RecordingNotifier()
        val service = PriceMonitorService(provider, notifier, listOf())

        assertEquals(250.0, service.getLowestPriceByMonth("GRU", "BKK", YearMonth.of(2026, 6), null))
        assertEquals(240.0, service.getLowestPriceByDateRange("GRU", "BKK", "2026-06-01", "2026-06-10", null))
    }

    private class SequentialPriceProvider(private val prices: List<Double>) : FlightPriceProvider {
        private val index = AtomicInteger(0)

        override suspend fun findLowestPrice(origin: String, destination: String, departureDate: String): BigDecimal? {
            val current = prices[index.getAndIncrement().coerceAtMost(prices.lastIndex)]
            return nullablePrice(current)
        }

        override suspend fun findLowestPriceByMonth(origin: String, destination: String, month: YearMonth, airlines: List<String>?): BigDecimal? = null
        override suspend fun findLowestPriceByDateRange(origin: String, destination: String, startDate: String, endDate: String, airlines: List<String>?): BigDecimal? = null
    }

    private class FailingPriceProvider : FlightPriceProvider {
        override suspend fun findLowestPrice(origin: String, destination: String, departureDate: String): BigDecimal? = null
        override suspend fun findLowestPriceByMonth(origin: String, destination: String, month: YearMonth, airlines: List<String>?): BigDecimal? = null
        override suspend fun findLowestPriceByDateRange(origin: String, destination: String, startDate: String, endDate: String, airlines: List<String>?): BigDecimal? = null
    }

    private class RecordingNotifier : EmailNotifier {
        var sentCount: Int = 0
            private set

        override fun sendAlert(subject: String, body: String, recipients: List<String>): EmailSendResult {
            sentCount += 1
            return EmailSendResult.Sent(recipients)
        }
    }
}

private fun nullablePrice(value: Double): BigDecimal? = listOf<BigDecimal?>(BigDecimal(value.toString()), null).first()

