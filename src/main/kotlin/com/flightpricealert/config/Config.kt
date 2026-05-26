package com.flightpricealert.config

data class AppConfig(
    val port: Int,
    val amadeusClientId: String?,
    val amadeusClientSecret: String?,
    val smtpHost: String?,
    val smtpPort: Int,
    val smtpUser: String?,
    val smtpPassword: String?,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val schedulerIntervalHours: Long
)

object Config {
    fun load(): AppConfig {
        val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

        val amadeusClientId = System.getenv("AMADEUS_CLIENT_ID")
        val amadeusClientSecret = System.getenv("AMADEUS_CLIENT_SECRET")

        val smtpHost = System.getenv("SMTP_HOST")
        val smtpPort = System.getenv("SMTP_PORT")?.toIntOrNull() ?: 587
        val smtpUser = System.getenv("SMTP_USER")
        val smtpPassword = System.getenv("SMTP_PASSWORD")

        val dbUrl = System.getenv("DB_URL") ?: "jdbc:h2:mem:flightdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        val dbUser = System.getenv("DB_USER") ?: "sa"
        val dbPassword = System.getenv("DB_PASSWORD") ?: ""

        val schedulerIntervalHours = System.getenv("SCHEDULER_INTERVAL_HOURS")?.toLongOrNull() ?: 6L

        return AppConfig(
            port = port,
            amadeusClientId = amadeusClientId,
            amadeusClientSecret = amadeusClientSecret,
            smtpHost = smtpHost,
            smtpPort = smtpPort,
            smtpUser = smtpUser,
            smtpPassword = smtpPassword,
            dbUrl = dbUrl,
            dbUser = dbUser,
            dbPassword = dbPassword,
            schedulerIntervalHours = schedulerIntervalHours
        )
    }
}


