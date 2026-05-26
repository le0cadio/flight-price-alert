package com.flightpricealert.email

import org.slf4j.LoggerFactory

class EmailService(private val cfg: ConfigHolder) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    fun sendAlert(subject: String, body: String, recipients: List<String>) {
        if (cfg.smtpHost.isNullOrBlank()) {
            log.info("SMTP not configured; simulated send. Subject={}, recipients={}", subject, recipients)
            return
        }

        log.info(
            "SMTP configured (host={}, port={}). Simulated send to {} with subject='{}'. Body='{}'",
            cfg.smtpHost,
            cfg.smtpPort,
            recipients,
            subject,
            body
        )
    }
}

data class ConfigHolder(
    val smtpHost: String?,
    val smtpPort: Int,
    val smtpUser: String?,
    val smtpPassword: String?
)



