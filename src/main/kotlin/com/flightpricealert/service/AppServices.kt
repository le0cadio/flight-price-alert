package com.flightpricealert.service

import com.flightpricealert.amadeus.AmadeusClient
import com.flightpricealert.config.AppConfig
import com.flightpricealert.email.ConfigHolder
import com.flightpricealert.email.EmailService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

object AppServices {
    private lateinit var monitorServiceRef: PriceMonitorService
    private lateinit var emailServiceRef: EmailService

    fun init(config: AppConfig) {
        val httpClient = HttpClient(CIO)
        val amadeusClient = AmadeusClient(httpClient, config.amadeusClientId, config.amadeusClientSecret)
        val emailService = EmailService(
            ConfigHolder(
                smtpHost = config.smtpHost,
                smtpPort = config.smtpPort,
                smtpUser = config.smtpUser,
                smtpPassword = config.smtpPassword
            )
        )
        emailServiceRef = emailService

        monitorServiceRef = PriceMonitorService(amadeusClient, emailService)
    }

    fun monitorService(): PriceMonitorService = monitorServiceRef

    fun emailService(): EmailService = emailServiceRef
}


