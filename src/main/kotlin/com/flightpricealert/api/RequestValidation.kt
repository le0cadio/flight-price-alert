package com.flightpricealert.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable

private val IATA_REGEX = Regex("^[A-Z]{3}$")
private val AIRLINE_REGEX = Regex("^[A-Z0-9]{2,3}$")
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

class ValidationException(val details: List<String>) : RuntimeException(details.joinToString("; "))

class NotFoundException(message: String) : RuntimeException(message)

@Serializable
data class ApiErrorResponse(
    val error: String,
    val details: List<String> = emptyList()
)

object RequestValidation {
    fun iata(value: String?, fieldName: String): String {
        val normalized = value?.trim()?.uppercase().orEmpty()
        if (!IATA_REGEX.matches(normalized)) {
            throw ValidationException(listOf("$fieldName must be a valid 3-letter IATA code"))
        }
        return normalized
    }

    fun airlineCodes(values: List<String>?): List<String>? {
        if (values == null) return null
        val normalized = values.map { it.trim().uppercase() }.filter { it.isNotBlank() }
        if (normalized.isEmpty()) return null
        if (normalized.any { !AIRLINE_REGEX.matches(it) }) {
            throw ValidationException(listOf("airlines must contain 2-3 character airline codes"))
        }
        return normalized.distinct()
    }

    fun positivePrice(value: Double?, fieldName: String): Double {
        if (value == null || !value.isFinite() || value <= 0.0) {
            throw ValidationException(listOf("$fieldName must be greater than zero"))
        }
        return value
    }

    fun optionalPositivePrice(value: Double?, fieldName: String): Double? {
        if (value == null) return null
        return positivePrice(value, fieldName)
    }

    fun month(value: String?, fieldName: String = "month"): YearMonth {
        val raw = value?.trim().orEmpty()
        try {
            return YearMonth.parse(raw)
        } catch (_: DateTimeParseException) {
            throw ValidationException(listOf("$fieldName must be in yyyy-MM format"))
        }
    }

    fun date(value: String?, fieldName: String): LocalDate {
        val raw = value?.trim().orEmpty()
        try {
            return LocalDate.parse(raw)
        } catch (_: DateTimeParseException) {
            throw ValidationException(listOf("$fieldName must be in yyyy-MM-dd format"))
        }
    }

    fun dateRange(startDate: String?, endDate: String?): Pair<LocalDate, LocalDate> {
        val start = date(startDate, "startDate")
        val end = date(endDate, "endDate")
        if (end.isBefore(start)) {
            throw ValidationException(listOf("endDate must be greater than or equal to startDate"))
        }
        return start to end
    }

    fun alertTravelWindow(departureDateFrom: String?, departureDateTo: String?): Pair<LocalDate, LocalDate?> {
        val start = date(departureDateFrom, "departureDateFrom")
        if (start.isBefore(LocalDate.now())) {
            throw ValidationException(listOf("departureDateFrom must not be in the past"))
        }
        val end = departureDateTo?.takeIf { it.isNotBlank() }?.let { date(it, "departureDateTo") }
        if (end != null && end.isBefore(start)) {
            throw ValidationException(listOf("departureDateTo must be greater than or equal to departureDateFrom"))
        }
        return start to end
    }

    fun recipients(values: List<String>?): List<String> {
        val normalized = values.orEmpty().map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalized.isEmpty()) {
            throw ValidationException(listOf("recipients cannot be empty"))
        }
        if (normalized.any { !EMAIL_REGEX.matches(it) }) {
            throw ValidationException(listOf("recipients must be valid email addresses"))
        }
        return normalized
    }
}

object SimpleRateLimiter {
    private const val WINDOW_MILLIS = 60_000L
    private data class Bucket(var windowStart: Long, var count: Int)
    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun allow(key: String, limitPerMinute: Int, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val bucket = buckets.computeIfAbsent(key) { Bucket(nowMillis, 0) }
        synchronized(bucket) {
            if (nowMillis - bucket.windowStart >= WINDOW_MILLIS) {
                bucket.windowStart = nowMillis
                bucket.count = 0
            }
            bucket.count += 1
            return bucket.count <= limitPerMinute
        }
    }

    @Suppress("unused")
    internal fun resetForTests() {
        buckets.clear()
    }
}

suspend inline fun ApplicationCall.respondApi(block: ApplicationCall.() -> Unit) {
    try {
        block()
    } catch (cause: ValidationException) {
        respond(HttpStatusCode.BadRequest, ApiErrorResponse("validation_error", cause.details))
    } catch (cause: NotFoundException) {
        respond(HttpStatusCode.NotFound, ApiErrorResponse("not_found", listOfNotNull(cause.message)))
    } catch (cause: IllegalArgumentException) {
        respond(HttpStatusCode.BadRequest, ApiErrorResponse("invalid_request", listOfNotNull(cause.message)))
    } catch (cause: Throwable) {
        application.environment.log.error("Unhandled API error", cause)
        respond(HttpStatusCode.InternalServerError, ApiErrorResponse("internal_error", listOf("Unexpected server error")))
    }
}

