package com.flightpricealert.repository

import com.flightpricealert.domain.PriceHistory
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

object PriceHistoryRepository {
    private val idSequence = AtomicInteger(1)
    private val history = CopyOnWriteArrayList<PriceHistory>()

    fun add(alertId: Int, price: Double, checkedAt: LocalDateTime = LocalDateTime.now()): PriceHistory {
        val item = PriceHistory(
            id = idSequence.getAndIncrement(),
            alertId = alertId,
            price = price,
            checkedAt = checkedAt
        )
        history.add(item)
        return item
    }

    fun listByAlert(alertId: Int): List<PriceHistory> = history.filter { it.alertId == alertId }
}


