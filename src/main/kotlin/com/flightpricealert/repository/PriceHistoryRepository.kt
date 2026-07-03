package com.flightpricealert.repository

import com.flightpricealert.domain.PriceHistory
import com.flightpricealert.domain.PriceStats
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

    fun stats(alertId: Int): PriceStats = DatabaseFactory.useConnection { connection ->
        connection.prepareStatement(
            """
            SELECT COUNT(*) AS cnt, MIN(price) AS min_price, AVG(price) AS avg_price
            FROM price_history
            WHERE alert_id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, alertId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    val count = resultSet.getInt("cnt")
                    val min = resultSet.getDouble("min_price").takeIf { !resultSet.wasNull() }
                    val average = resultSet.getDouble("avg_price").takeIf { !resultSet.wasNull() }
                    PriceStats(count = count, min = min, average = average)
                } else {
                    PriceStats(count = 0, min = null, average = null)
                }
            }
        }
    }

    fun lastPrice(alertId: Int): Double? = DatabaseFactory.useConnection { connection ->
        connection.prepareStatement(
            """
            SELECT price
            FROM price_history
            WHERE alert_id = ?
            ORDER BY checked_at DESC, id DESC
            LIMIT 1
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, alertId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.getDouble("price") else null
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


