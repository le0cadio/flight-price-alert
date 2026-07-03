package com.flightpricealert.repository

import com.flightpricealert.domain.FlightAlert
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.sql.ResultSet

object AlertRepository {
    private val json = Json { encodeDefaults = true }

    fun create(
        origin: String,
        destination: String,
        departureDateFrom: LocalDate,
        departureDateTo: LocalDate?,
        targetPrice: Double?,
        airlines: List<String>?
    ): FlightAlert {
        val now = LocalDateTime.now()
        val airlinesValue = airlines?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(ListSerializer(String.serializer()), it) }

        return DatabaseFactory.useConnection { connection ->
            connection.prepareStatement(
                """
                INSERT INTO flight_alerts (origin, destination, departure_date_from, departure_date_to, target_price, airlines, active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                java.sql.Statement.RETURN_GENERATED_KEYS
            ).use { statement ->
                statement.setString(1, origin)
                statement.setString(2, destination)
                statement.setDate(3, java.sql.Date.valueOf(departureDateFrom))
                statement.setDate(4, departureDateTo?.let { java.sql.Date.valueOf(it) })
                statement.setObjectOrNull(5, targetPrice)
                statement.setString(6, airlinesValue)
                statement.setBoolean(7, true)
                statement.setTimestamp(8, java.sql.Timestamp.valueOf(now))
                statement.executeUpdate()

                val generatedId = statement.generatedKeys.use { keys ->
                    if (keys.next()) keys.getInt(1) else error("Failed to create alert")
                }

                findById(connection, generatedId) ?: error("Alert was inserted but could not be reloaded")
            }
        }
    }

    fun listAll(): List<FlightAlert> = DatabaseFactory.useConnection { connection ->
        findAll(connection, onlyActive = false)
    }

    fun listActive(): List<FlightAlert> = DatabaseFactory.useConnection { connection ->
        findAll(connection, onlyActive = true)
    }

    fun findById(id: Int): FlightAlert? = DatabaseFactory.useConnection { connection ->
        findById(connection, id)
    }

    fun update(
        id: Int,
        origin: String,
        destination: String,
        departureDateFrom: LocalDate,
        departureDateTo: LocalDate?,
        targetPrice: Double?,
        airlines: List<String>?,
        active: Boolean
    ): FlightAlert? {
        val airlinesValue = airlines?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(ListSerializer(String.serializer()), it) }

        return DatabaseFactory.useConnection { connection ->
            connection.prepareStatement(
                """
                UPDATE flight_alerts
                SET origin = ?, destination = ?, departure_date_from = ?, departure_date_to = ?, target_price = ?, airlines = ?, active = ?
                WHERE id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, origin)
                statement.setString(2, destination)
                statement.setDate(3, java.sql.Date.valueOf(departureDateFrom))
                statement.setDate(4, departureDateTo?.let { java.sql.Date.valueOf(it) })
                statement.setObjectOrNull(5, targetPrice)
                statement.setString(6, airlinesValue)
                statement.setBoolean(7, active)
                statement.setInt(8, id)
                val rows = statement.executeUpdate()
                if (rows == 0) return@useConnection null

                findById(connection, id)
            }
        }
    }

    fun setActive(id: Int, active: Boolean): FlightAlert? {
        return DatabaseFactory.useConnection { connection ->
            connection.prepareStatement(
                "UPDATE flight_alerts SET active = ? WHERE id = ?"
            ).use { statement ->
                statement.setBoolean(1, active)
                statement.setInt(2, id)
                val rows = statement.executeUpdate()
                if (rows == 0) return@useConnection null
                findById(connection, id)
            }
        }
    }

    fun delete(id: Int): Boolean = DatabaseFactory.useConnection { connection ->
        connection.prepareStatement("DELETE FROM flight_alerts WHERE id = ?").use { statement ->
            statement.setInt(1, id)
            statement.executeUpdate() > 0
        }
    }

    private fun findAll(connection: java.sql.Connection, onlyActive: Boolean): List<FlightAlert> = run {
        val sql = if (onlyActive) {
            "SELECT * FROM flight_alerts WHERE active = TRUE ORDER BY id"
        } else {
            "SELECT * FROM flight_alerts ORDER BY id"
        }

        connection.prepareStatement(sql).use { statement ->
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(resultSet.toFlightAlert())
                    }
                }
            }
        }
    }

    private fun findById(connection: java.sql.Connection, id: Int): FlightAlert? =
        connection.prepareStatement("SELECT * FROM flight_alerts WHERE id = ?").use { statement ->
            statement.setInt(1, id)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.toFlightAlert() else null
            }
        }

    private fun java.sql.PreparedStatement.setObjectOrNull(index: Int, value: Double?) {
        if (value == null) {
            setNull(index, java.sql.Types.DOUBLE)
        } else {
            setDouble(index, value)
        }
    }

    private fun ResultSet.toFlightAlert(): FlightAlert {
        val airlinesJson = getString("airlines")
        val airlines = airlinesJson
            ?.takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString(ListSerializer(String.serializer()), it) }

        val targetPrice = getDouble("target_price").takeIf { !wasNull() }

        return FlightAlert(
            id = getInt("id"),
            origin = getString("origin"),
            destination = getString("destination"),
            departureDateFrom = getDate("departure_date_from").toLocalDate(),
            departureDateTo = getDate("departure_date_to")?.toLocalDate(),
            targetPrice = targetPrice,
            airlines = airlines,
            active = getBoolean("active"),
            createdAt = getTimestamp("created_at")?.toLocalDateTime()
        )
    }
}
