package com.flightpricealert.repository

import com.flightpricealert.domain.PriceHistory
import java.time.LocalDateTime

object PriceHistoryRepository {
    fun add(alertId: Int, price: Double, checkedAt: LocalDateTime = LocalDateTime.now()): PriceHistory {
        return DatabaseFactory.useConnection { connection ->
            connection.prepareStatement(
                """
                INSERT INTO price_history (alert_id, price, checked_at)
                VALUES (?, ?, ?)
                """.trimIndent(),
                java.sql.Statement.RETURN_GENERATED_KEYS
            ).use { statement ->
                statement.setInt(1, alertId)
                statement.setDouble(2, price)
                statement.setTimestamp(3, java.sql.Timestamp.valueOf(checkedAt))
                statement.executeUpdate()

                val generatedId = statement.generatedKeys.use { keys ->
                    if (keys.next()) keys.getInt(1) else error("Failed to create price history row")
                }

                PriceHistory(
                    id = generatedId,
                    alertId = alertId,
                    price = price,
                    checkedAt = checkedAt
                )
            }
        }
    }

    fun listByAlert(alertId: Int): List<PriceHistory> = DatabaseFactory.useConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id, alert_id, price, checked_at
            FROM price_history
            WHERE alert_id = ?
            ORDER BY checked_at ASC, id ASC
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, alertId)
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(
                            PriceHistory(
                                id = resultSet.getInt("id"),
                                alertId = resultSet.getInt("alert_id"),
                                price = resultSet.getDouble("price"),
                                checkedAt = resultSet.getTimestamp("checked_at").toLocalDateTime()
                            )
                        )
                    }
                }
            }
        }
    }
}


