package com.flightpricealert.email

import com.flightpricealert.domain.PriceStats
import com.flightpricealert.domain.Recommendation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object EmailTemplate {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
    private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun alertHtmlBody(
        origin: String,
        destination: String,
        currentPrice: Double,
        recommendation: Recommendation,
        stats: PriceStats,
        departureDateFrom: LocalDate,
        departureDateTo: LocalDate?,
        targetPrice: Double? = null,
        foundDate: LocalDateTime = LocalDateTime.now()
    ): String {
        val formattedDate = foundDate.format(dateFormatter)
        val travelWindow = if (departureDateTo != null && departureDateTo != departureDateFrom) {
            "${departureDateFrom.format(dayFormatter)} a ${departureDateTo.format(dayFormatter)}"
        } else {
            departureDateFrom.format(dayFormatter)
        }
        val verdictColor = when (recommendation) {
            Recommendation.BEST_PRICE, Recommendation.GOOD_PRICE -> "#2ecc71"
            Recommendation.TRENDING_DOWN -> "#f1c40f"
            Recommendation.NONE -> "#95a5a6"
        }

        val statsRows = buildString {
            stats.average?.let {
                append("<tr><td style=\"padding: 10px;\"><strong>Média histórica:</strong></td>")
                append("<td style=\"padding: 10px; text-align: right;\">R$ ${money(it)}</td></tr>")
            }
            stats.min?.let {
                append("<tr><td style=\"padding: 10px;\"><strong>Menor preço já visto:</strong></td>")
                append("<td style=\"padding: 10px; text-align: right;\">R$ ${money(it)}</td></tr>")
            }
            append("<tr><td style=\"padding: 10px;\"><strong>Checagens realizadas:</strong></td>")
            append("<td style=\"padding: 10px; text-align: right;\">${stats.count}</td></tr>")
            targetPrice?.let {
                append("<tr><td style=\"padding: 10px;\"><strong>Seu teto de preço:</strong></td>")
                append("<td style=\"padding: 10px; text-align: right;\">R$ ${money(it)}</td></tr>")
            }
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; background: #f9f9f9; border-radius: 8px; }
                    .header { background: #3498db; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; }
                    .content { background: white; padding: 20px; border-radius: 0 0 8px 8px; }
                    .verdict-box { background: $verdictColor; color: white; padding: 15px; border-radius: 5px; text-align: center; margin: 15px 0; }
                    .route { font-size: 24px; font-weight: bold; }
                    .price { font-size: 32px; font-weight: bold; }
                    .verdict-title { font-size: 20px; font-weight: bold; }
                    .footer { margin-top: 20px; font-size: 12px; color: #888; text-align: center; }
                    .label { color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>✈️ Flight Price Alert</h1>
                    </div>
                    <div class="content">
                        <div class="route" style="text-align: center; margin: 20px 0;">
                            $origin → $destination
                        </div>
                        <p style="text-align: center; color: #666;">Viagem: $travelWindow</p>

                        <div class="verdict-box">
                            <div class="verdict-title">${recommendation.emoji} ${recommendation.title}</div>
                            <div class="price">R$ ${money(currentPrice)}</div>
                            <div class="label" style="color: white;">${recommendation.message}</div>
                        </div>

                        <table style="width: 100%; margin: 15px 0;">
                            $statsRows
                            <tr>
                                <td style="padding: 10px;"><strong>Data da verificação:</strong></td>
                                <td style="padding: 10px; text-align: right;">$formattedDate</td>
                            </tr>
                        </table>

                        <p style="margin-top: 20px; color: #666;">
                            Visite o site da Amadeus ou sua agência de viagens preferida para completar a compra.
                        </p>
                    </div>
                    <div class="footer">
                        <p>Flight Price Alert - Monitoramento automático de preços</p>
                        <p>Este é um alerta automático. Não responda este e-mail.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
