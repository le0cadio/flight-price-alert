package com.flightpricealert.repository

import com.flightpricealert.domain.NotificationLog
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

object NotificationLogRepository {
    private val idSequence = AtomicInteger(1)
    private val logs = CopyOnWriteArrayList<NotificationLog>()

    fun add(alertId: Int, price: Double, recipient: String? = null, sentAt: LocalDateTime = LocalDateTime.now()): NotificationLog {
        val item = NotificationLog(
            id = idSequence.getAndIncrement(),
            alertId = alertId,
            price = price,
            sentAt = sentAt,
            recipient = recipient
        )
        logs.add(item)
        return item
    }

    fun lastSentPrice(alertId: Int): Double? = logs
        .filter { it.alertId == alertId }
        .maxByOrNull { it.sentAt }
        ?.price
}


