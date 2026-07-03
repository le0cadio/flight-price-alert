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
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PriceMonitorServiceTest {
    private val testConfig = AppConfig(
        appEnvironment = AppEnvironment.LOCAL,
        port = 8080,
        amadeusClientId = null,
        amadeusClientSecret = null,
        amadeusBaseUrl = "https://test.api.amadeus.com",
        amadeusMaxDatesPerCheck = 10,
        alertRecipients = listOf("user@example.com"),
        smtpHost = null,
        smtpPort = 587,
        smtpUser = null,
        smtpPassword = null,
        dbUrl = "jdbc:h2:mem:monitor-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        dbUser = "sa",
        dbPassword = "",
        schedulerIntervalHours = 6L,
        enableInternalScheduler = false,
        logLevel = "DEBUG",
        requestTimeoutMillis = 5000L,
        requestRetryCount = 1,
        rateLimitPerMinute = 120
    )

    private val futureDate: LocalDate = LocalDate.now().plusMonths(4)

    @BeforeTest
    fun setup() {
        DatabaseFactory.init(testConfig)
        DatabaseFactory.reset()
    }

    private fun createAlert(
        origin: String = "GRU",
        destination: String = "BKK",
        targetPrice: Double? = null
    ) = AlertRepository.create(
        origin = origin,
        destination = destination,
        departureDateFrom = futureDate,
        departureDateTo = null,
        targetPrice = targetPrice,
        airlines = null
    )

    @Test
    fun `does not notify while still building baseline history`() = runBlocking {
        val alert = createAlert()
        val provider = SequentialPriceProvider(listOf(1000.0, 950.0, 900.0))
        val notifier = RecordingNotifier()
        val service = PriceMonitorService(provider, notifier, listOf("user@example.com"))

        repeat(3) { service.checkAlert(alert) }

        assertEquals(0, notifier.sentCount)
        assertEquals(3, PriceHistoryRepository.listByAlert(requireNotNull(alert.id)).size)
    }

    @Test
    fun `notifies best price once baseline exists and a new low is found`() = runBlocking {
        val alert = createAlert()
        val alertId = requireNotNull(alert.id)
        listOf(1000.0, 980.0, 960.0).forEach { PriceHistoryRepository.add(alertId, it) }

        val provider = SequentialPriceProvider(listOf(700.0))
        val notifier = RecordingNotifier()
        val service = PriceMonitorService(provider, notifier, listOf("user@example.com"))

        val price = service.checkAlert(alert)

        assertEquals(700.0, price)
        assertEquals(1, notifier.sentCount)
        assertEquals(700.0, NotificationLogRepository.lastSentPrice(alertId))
    }

    @Test
    fun `does not notify again unless the price improves further`() = runBlocking {
        val alert = createAlert()
        val alertId = requireNotNull(alert.id)
        listOf(1000.0, 980.0, 960.0).forEach { PriceHistoryRepository.add(alertId, it) }

        val provider = SequentialPriceProvider(listOf(700.0, 700.0, 650.0))
        val notifier = RecordingNotifier()
        val service = PriceMonitorService(provider, notifier, listOf("user@example.com"))

        service.checkAlert(alert)
        service.checkAlert(alert)
        service.checkAlert(alert)

        assertEquals(2, notifier.sentCount)
        assertEquals(650.0, NotificationLogRepository.lastSentPrice(alertId))
    }

    @Test
    fun `never notifies above the optional target price ceiling`() = runBlocking {
        val alert = createAlert(targetPrice = 500.0)
        val alertId = requireNotNull(alert.id)
        listOf(1000.0, 980.0, 960.0).forEach { PriceHistoryRepository.add(alertId, it) }

        // Best price ever seen (700 < min 960) but still above the 500 ceiling.
        val provider = SequentialPriceProvider(listOf(700.0))
        val notifier = RecordingNotifier()
        val service = PriceMonitorService(provider, notifier, listOf("user@example.com"))

        service.checkAlert(alert)

        assertEquals(0, notifier.sentCount)
    }

    @Test
    fun `skips the cycle and records nothing when the provider has no price`() = runBlocking {
        val alert = createAlert(origin = "SAO", destination = "MIA")
        val provider = FailingPriceProvider()
        val notifier = RecordingNotifier()
        val service = PriceMonitorService(provider, notifier, listOf("user@example.com"))

        val price = service.checkAlert(alert)

        assertNull(price)
        assertEquals(0, PriceHistoryRepository.listByAlert(requireNotNull(alert.id)).size)
        assertEquals(0, notifier.sentCount)
    }

    @Test
    fun `returns explicit month and range prices from provider`() = runBlocking {
        val provider = object : FlightPriceProvider {
            override suspend fun findLowestPrice(origin: String, destination: String, departureDate: String): BigDecimal? = BigDecimal("300.0")
            override suspend fun findLowestPriceByMonth(origin: String, destination: String, month: YearMonth, airlines: List<String>?): BigDecimal? = BigDecimal("250.0")
            override suspend fun findLowestPriceByDateRange(origin: String, destination: String, startDate: String, endDate: String, airlines: List<String>?): BigDecimal? = BigDecimal("240.0")
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
            return BigDecimal(current.toString())
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
