package com.flightpricealert.service

import com.flightpricealert.amadeus.AmadeusClient
import com.flightpricealert.config.AppConfig
import com.flightpricealert.email.ConfigHolder
import com.flightpricealert.email.EmailService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

object AppServices {
    private lateinit var appConfigRef: AppConfig
    private lateinit var monitorServiceRef: PriceMonitorService
    private lateinit var emailServiceRef: EmailService

    fun init(config: AppConfig) {
        appConfigRef = config
        val httpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeoutMillis
                connectTimeoutMillis = config.requestTimeoutMillis
                socketTimeoutMillis = config.requestTimeoutMillis
            }
        }
        val amadeusClient = AmadeusClient(
            httpClient,
            config.amadeusClientId,
            config.amadeusClientSecret,
            requestRetryCount = config.requestRetryCount,
            baseUrl = config.amadeusBaseUrl,
            maxDatesPerCheck = config.amadeusMaxDatesPerCheck
        )
        val emailService = EmailService(
            ConfigHolder(
                smtpHost = config.smtpHost,
                smtpPort = config.smtpPort,
                smtpUser = config.smtpUser,
                smtpPassword = config.smtpPassword
            )
        )
        emailServiceRef = emailService

        monitorServiceRef = PriceMonitorService(amadeusClient, emailService, config.alertRecipients)
    }

    fun config(): AppConfig = appConfigRef

    fun monitorService(): PriceMonitorService = monitorServiceRef

    fun emailService(): EmailService = emailServiceRef
}


