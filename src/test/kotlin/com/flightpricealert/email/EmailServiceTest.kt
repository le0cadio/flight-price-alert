package com.flightpricealert.email

import javax.mail.internet.MimeMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailServiceTest {
    @Test
    fun `sendAlert returns sent and builds html message`() {
        var captured: MimeMessage? = null
        val service = EmailService(
            ConfigHolder(
                smtpHost = "smtp.example.com",
                smtpPort = 587,
                smtpUser = "alerts@example.com",
                smtpPassword = "secret"
            ),
            mailDispatcher = object : MailDispatcher {
                override fun send(message: MimeMessage) {
                    message.saveChanges()
                    captured = message
                }
            }
        )

        val result = service.sendAlert(
            subject = "Price drop",
            body = "<html><body>ok</body></html>",
            recipients = listOf("user@example.com")
        )

        assertTrue(result is EmailSendResult.Sent)
        assertEquals(listOf("user@example.com"), result.recipients)
        assertEquals("Price drop", captured?.subject)
        assertTrue(captured?.contentType?.contains("text/html") == true)
        assertEquals("alerts@example.com", requireNotNull(captured).from.firstOrNull()?.toString())
    }

    @Test
    fun `sendAlert returns skipped when smtp is missing`() {
        val service = EmailService(
            ConfigHolder(
                smtpHost = null,
                smtpPort = 587,
                smtpUser = null,
                smtpPassword = null
            )
        )

        val result = service.sendAlert("subject", "body", listOf("user@example.com"))
        assertTrue(result is EmailSendResult.Skipped)
    }

    @Test
    fun `sendAlert returns failed when dispatcher throws`() {
        val service = EmailService(
            ConfigHolder(
                smtpHost = "smtp.example.com",
                smtpPort = 587,
                smtpUser = "alerts@example.com",
                smtpPassword = "secret"
            ),
            mailDispatcher = object : MailDispatcher {
                override fun send(message: MimeMessage) {
                    throw RuntimeException("boom")
                }
            }
        )

        val result = service.sendAlert("subject", "body", listOf("user@example.com"))
        assertTrue(result is EmailSendResult.Failed)
        assertTrue(result.reason.contains("boom"))
    }
}

