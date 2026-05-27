package com.flightpricealert.email

import org.slf4j.LoggerFactory
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

sealed class EmailSendResult {
    data class Sent(val recipients: List<String>) : EmailSendResult()
    data class Skipped(val reason: String) : EmailSendResult()
    data class Failed(val reason: String) : EmailSendResult()
}

fun interface MailDispatcher {
    fun send(message: MimeMessage)
}

interface EmailNotifier {
    fun sendAlert(subject: String, body: String, recipients: List<String>): EmailSendResult
}

class EmailService(
    private val cfg: ConfigHolder,
    private val mailDispatcher: MailDispatcher = MailDispatcher { message -> Transport.send(message) }
) : EmailNotifier {
    private val log = LoggerFactory.getLogger(EmailService::class.java)
    private val senderEmail = cfg.smtpUser ?: "no-reply@flight-alert.local"

    override fun sendAlert(subject: String, body: String, recipients: List<String>): EmailSendResult {
        if (cfg.smtpHost.isNullOrBlank() || recipients.isEmpty()) {
            log.warn("SMTP not configured or no recipients; skipping. Subject={}", subject)
            return EmailSendResult.Skipped("SMTP not configured or recipients empty")
        }

        val smtpUser = cfg.smtpUser?.takeIf { it.isNotBlank() }
        val smtpPassword = cfg.smtpPassword?.takeIf { it.isNotBlank() }
        val shouldAuthenticate = smtpUser != null && smtpPassword != null

        val props = Properties().apply {
            put("mail.smtp.host", cfg.smtpHost)
            put("mail.smtp.port", cfg.smtpPort.toString())
            put("mail.smtp.auth", shouldAuthenticate.toString())
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
            put("mail.smtp.writetimeout", "10000")
        }

        val session = if (shouldAuthenticate) {
            Session.getInstance(
                props,
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(smtpUser, smtpPassword)
                    }
                }
            )
        } else {
            Session.getInstance(props)
        }

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(senderEmail))
                setRecipients(
                    Message.RecipientType.TO,
                    recipients.map { InternetAddress(it) }.toTypedArray()
                )
                this.subject = subject

                if (body.contains("<html", ignoreCase = true)) {
                    setContent(body, "text/html; charset=UTF-8")
                } else {
                    setText(body, "UTF-8")
                }
            }

            mailDispatcher.send(message)
            log.info("Email sent successfully from {} to {} with subject='{}'", senderEmail, recipients, subject)
            return EmailSendResult.Sent(recipients)
        } catch (t: Throwable) {
            log.error("Failed to send email via SMTP host={} port={}", cfg.smtpHost, cfg.smtpPort, t)
            return EmailSendResult.Failed(t.message ?: "SMTP send failed")
        }
    }
}

data class ConfigHolder(
    val smtpHost: String?,
    val smtpPort: Int,
    val smtpUser: String?,
    val smtpPassword: String?
)



