package com.flightpricealert.email

import org.slf4j.LoggerFactory

class EmailService(private val cfg: ConfigHolder) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)
    private val senderEmail = cfg.smtpUser ?: "no-reply@flight-alert.local"

    fun sendAlert(subject: String, body: String, recipients: List<String>) {
        if (cfg.smtpHost.isNullOrBlank() || recipients.isEmpty()) {
            log.warn("SMTP not configured or no recipients; skipping. Subject={}", subject)
            return
        }

        log.info(
            "SMTP configured locally (host={}, port={}). Simulated send from {} to {} subject='{}' body='{}'",
            cfg.smtpHost,
            cfg.smtpPort,
            senderEmail,
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



