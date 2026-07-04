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
    val amadeusBaseUrl: String,
    val amadeusMaxDatesPerCheck: Int,
    val alertRecipients: List<String>,
    val smtpHost: String?,
    val smtpPort: Int,
    val smtpUser: String?,
    val smtpPassword: String?,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val schedulerIntervalHours: Long,
    val enableInternalScheduler: Boolean,
    val logLevel: String,
    val requestTimeoutMillis: Long,
    val requestRetryCount: Int,
    val rateLimitPerMinute: Int
)

object Config {
    // CI systems (e.g. GitHub Actions) inject unset secrets as empty strings, so a blank
    // variable must fall back to the default exactly like an absent one.
    private fun env(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }

    fun load(): AppConfig {
        val appEnvironment = AppEnvironment.from(env("APP_ENV"))
        val port = env("PORT")?.toIntOrNull() ?: 8080

        val amadeusClientId = env("AMADEUS_CLIENT_ID")
        val amadeusClientSecret = env("AMADEUS_CLIENT_SECRET")
        val amadeusBaseUrl = env("AMADEUS_BASE_URL")?.removeSuffix("/") ?: "https://test.api.amadeus.com"
        val amadeusMaxDatesPerCheck = env("AMADEUS_MAX_DATES_PER_CHECK")?.toIntOrNull() ?: 10
        val alertRecipients = parseRecipients(
            env("ALERT_RECIPIENTS") ?: env("ALERT_RECIPIENT")
        )

        val smtpHost = env("SMTP_HOST")
        val smtpPort = env("SMTP_PORT")?.toIntOrNull() ?: 587
        val smtpUser = env("SMTP_USER")
        val smtpPassword = env("SMTP_PASSWORD")

        val dbUrl = env("DB_URL") ?: "jdbc:h2:file:./data/flightdb;AUTO_SERVER=TRUE;MODE=PostgreSQL"
        val dbUser = env("DB_USER") ?: "sa"
        val dbPassword = env("DB_PASSWORD") ?: ""

        val schedulerIntervalHours = env("SCHEDULER_INTERVAL_HOURS")?.toLongOrNull() ?: 6L
        val enableInternalScheduler = env("ENABLE_INTERNAL_SCHEDULER")?.toBooleanStrictOrNull() ?: false
        val logLevel = env("LOG_LEVEL") ?: if (appEnvironment == AppEnvironment.PROD) "INFO" else "DEBUG"
        val requestTimeoutMillis = env("HTTP_TIMEOUT_MS")?.toLongOrNull()
            ?: if (appEnvironment == AppEnvironment.PROD) 15_000L else 10_000L
        val requestRetryCount = env("HTTP_RETRY_COUNT")?.toIntOrNull()
            ?: if (appEnvironment == AppEnvironment.PROD) 3 else 1
        val rateLimitPerMinute = env("RATE_LIMIT_PER_MINUTE")?.toIntOrNull()
            ?: if (appEnvironment == AppEnvironment.PROD) 30 else 120

        return AppConfig(
            appEnvironment = appEnvironment,
            port = port,
            amadeusClientId = amadeusClientId,
            amadeusClientSecret = amadeusClientSecret,
            amadeusBaseUrl = amadeusBaseUrl,
            amadeusMaxDatesPerCheck = amadeusMaxDatesPerCheck,
            alertRecipients = alertRecipients,
            smtpHost = smtpHost,
            smtpPort = smtpPort,
            smtpUser = smtpUser,
            smtpPassword = smtpPassword,
            dbUrl = dbUrl,
            dbUser = dbUser,
            dbPassword = dbPassword,
            schedulerIntervalHours = schedulerIntervalHours,
            enableInternalScheduler = enableInternalScheduler,
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


