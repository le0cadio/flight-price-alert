package com.flightpricealert.config

enum class AppEnvironment {
    LOCAL,
    DEV,
    PROD;

    companion object {
        fun from(raw: String?): AppEnvironment {
            return when (raw?.trim()?.lowercase()) {
                "prod", "production" -> PROD
                "dev", "development" -> DEV
                else -> LOCAL
            }
        }
    }
}

data class AppConfig(
    val appEnvironment: AppEnvironment,
    val port: Int,
    val amadeusClientId: String?,
    val amadeusClientSecret: String?,
    val alertRecipients: List<String>,
    val smtpHost: String?,
    val smtpPort: Int,
    val smtpUser: String?,
    val smtpPassword: String?,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val schedulerIntervalHours: Long,
    val logLevel: String,
    val requestTimeoutMillis: Long,
    val requestRetryCount: Int,
    val rateLimitPerMinute: Int
)

object Config {
    fun load(): AppConfig {
        val appEnvironment = AppEnvironment.from(System.getenv("APP_ENV"))
        val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

        val amadeusClientId = System.getenv("AMADEUS_CLIENT_ID")
        val amadeusClientSecret = System.getenv("AMADEUS_CLIENT_SECRET")
        val alertRecipients = parseRecipients(
            System.getenv("ALERT_RECIPIENTS") ?: System.getenv("ALERT_RECIPIENT")
        )

        val smtpHost = System.getenv("SMTP_HOST")
        val smtpPort = System.getenv("SMTP_PORT")?.toIntOrNull() ?: 587
        val smtpUser = System.getenv("SMTP_USER")
        val smtpPassword = System.getenv("SMTP_PASSWORD")

        val dbUrl = System.getenv("DB_URL") ?: "jdbc:h2:file:./data/flightdb;AUTO_SERVER=TRUE;MODE=PostgreSQL"
        val dbUser = System.getenv("DB_USER") ?: "sa"
        val dbPassword = System.getenv("DB_PASSWORD") ?: ""

        val schedulerIntervalHours = System.getenv("SCHEDULER_INTERVAL_HOURS")?.toLongOrNull() ?: 6L
        val logLevel = System.getenv("LOG_LEVEL") ?: if (appEnvironment == AppEnvironment.PROD) "INFO" else "DEBUG"
        val requestTimeoutMillis = System.getenv("HTTP_TIMEOUT_MS")?.toLongOrNull()
            ?: if (appEnvironment == AppEnvironment.PROD) 15_000L else 10_000L
        val requestRetryCount = System.getenv("HTTP_RETRY_COUNT")?.toIntOrNull()
            ?: if (appEnvironment == AppEnvironment.PROD) 3 else 1
        val rateLimitPerMinute = System.getenv("RATE_LIMIT_PER_MINUTE")?.toIntOrNull()
            ?: if (appEnvironment == AppEnvironment.PROD) 30 else 120

        return AppConfig(
            appEnvironment = appEnvironment,
            port = port,
            amadeusClientId = amadeusClientId,
            amadeusClientSecret = amadeusClientSecret,
            alertRecipients = alertRecipients,
            smtpHost = smtpHost,
            smtpPort = smtpPort,
            smtpUser = smtpUser,
            smtpPassword = smtpPassword,
            dbUrl = dbUrl,
            dbUser = dbUser,
            dbPassword = dbPassword,
            schedulerIntervalHours = schedulerIntervalHours,
            logLevel = logLevel,
            requestTimeoutMillis = requestTimeoutMillis,
            requestRetryCount = requestRetryCount,
            rateLimitPerMinute = rateLimitPerMinute
        )
    }

    private fun parseRecipients(raw: String?): List<String> {
        return raw
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()
    }
}


