package com.flightpricealert.repository

import com.flightpricealert.config.AppConfig
import com.flightpricealert.config.AppEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.time.LocalDate
import java.time.LocalDateTime

class RepositoryPersistenceTest {
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
        dbUrl = "jdbc:h2:mem:flightdb_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        dbUser = "sa",
        dbPassword = "",
        schedulerIntervalHours = 6L,
        enableInternalScheduler = false,
        logLevel = "DEBUG",
        requestTimeoutMillis = 5000L,
        requestRetryCount = 1,
        rateLimitPerMinute = 120
    )

    private val departureDateFrom: LocalDate = LocalDate.of(2026, 11, 1)
    private val departureDateTo: LocalDate = LocalDate.of(2026, 11, 15)

    @BeforeTest
    fun setup() {
        DatabaseFactory.init(testConfig)
        DatabaseFactory.reset()
    }

    @Test
    fun `alert repository persists create update toggle and delete`() {
        val created = AlertRepository.create(
            origin = "GRU",
            destination = "BKK",
            departureDateFrom = departureDateFrom,
            departureDateTo = departureDateTo,
            targetPrice = 3500.0,
            airlines = listOf("LA", "AF")
        )

        assertNotNull(created.id)
        assertEquals(departureDateFrom, created.departureDateFrom)
        assertEquals(departureDateTo, created.departureDateTo)
        assertEquals(1, AlertRepository.listAll().size)
        assertEquals(1, AlertRepository.listActive().size)

        val alertId: Int = requireNotNull(created.id)

        val updated = AlertRepository.update(
            id = alertId,
            origin = "GRU",
            destination = "NRT",
            departureDateFrom = departureDateFrom,
            departureDateTo = null,
            targetPrice = null,
            airlines = listOf("NH"),
            active = false
        )

        assertNotNull(updated)
        assertEquals("NRT", updated.destination)
        assertNull(updated.departureDateTo)
        assertNull(updated.targetPrice)
        assertTrue(updated.airlines?.contains("NH") == true)
        assertEquals(0, AlertRepository.listActive().size)

        val toggled = AlertRepository.setActive(alertId, true)
        assertNotNull(toggled)
        assertTrue(toggled.active)

        assertTrue(AlertRepository.delete(alertId))
        assertNull(AlertRepository.findById(alertId))
    }

    @Test
    fun `history and notification repositories persist rows, last sent price and stats`() {
        val alert = AlertRepository.create(
            origin = "SAO",
            destination = "MIA",
            departureDateFrom = departureDateFrom,
            departureDateTo = null,
            targetPrice = 1500.0,
            airlines = null
        )

        val firstTime = LocalDateTime.of(2026, 1, 1, 10, 0)
        val secondTime = LocalDateTime.of(2026, 1, 2, 10, 0)
        val alertId: Int = requireNotNull(alert.id)

        val firstHistory = PriceHistoryRepository.add(alertId, 1700.0, firstTime)
        val secondHistory = PriceHistoryRepository.add(alertId, 1500.0, secondTime)

        assertNotNull(firstHistory.id)
        assertNotNull(secondHistory.id)
        assertEquals(listOf(1700.0, 1500.0), PriceHistoryRepository.listByAlert(alertId).map { it.price })
        assertEquals(1500.0, PriceHistoryRepository.lastPrice(alertId))

        val stats = PriceHistoryRepository.stats(alertId)
        assertEquals(2, stats.count)
        assertEquals(1500.0, stats.min)
        assertEquals(1600.0, stats.average)

        NotificationLogRepository.add(alertId, 1700.0, recipient = "test@example.com", sentAt = firstTime)
        NotificationLogRepository.add(alertId, 1500.0, recipient = "test@example.com", sentAt = secondTime)

        assertEquals(1500.0, NotificationLogRepository.lastSentPrice(alertId))
    }
}
