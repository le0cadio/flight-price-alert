package com.flightpricealert.repository

import com.flightpricealert.domain.NotificationLog
import java.time.LocalDateTime

object NotificationLogRepository {
    fun add(alertId: Int, price: Double, recipient: String? = null, sentAt: LocalDateTime = LocalDateTime.now()): NotificationLog {
        return DatabaseFactory.useConnection { connection ->
            connection.prepareStatement(
                """
                INSERT INTO notification_logs (alert_id, price, sent_at, recipient)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                java.sql.Statement.RETURN_GENERATED_KEYS
            ).use { statement ->
                statement.setInt(1, alertId)
                statement.setDouble(2, price)
                statement.setTimestamp(3, java.sql.Timestamp.valueOf(sentAt))
                statement.setString(4, recipient)
                statement.executeUpdate()

                val generatedId = statement.generatedKeys.use { keys ->
                    if (keys.next()) keys.getInt(1) else error("Failed to create notification log row")
                }

                NotificationLog(
                    id = generatedId,
                    alertId = alertId,
                    price = price,
                    sentAt = sentAt,
                    recipient = recipient
                )
            }
        }
    }

    fun lastSentPrice(alertId: Int): Double? = DatabaseFactory.useConnection { connection ->
        connection.prepareStatement(
            """
            SELECT price
            FROM notification_logs
            WHERE alert_id = ?
            ORDER BY sent_at DESC, id DESC
            LIMIT 1
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, alertId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getDouble("price") else null
            }
        }
    }
}


