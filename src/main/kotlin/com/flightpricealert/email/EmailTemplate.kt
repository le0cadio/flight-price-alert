package com.flightpricealert.email

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object EmailTemplate {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    fun alertHtmlBody(
        origin: String,
        destination: String,
        currentPrice: Double,
        targetPrice: Double,
        foundDate: LocalDateTime = LocalDateTime.now()
    ): String {
        val formattedDate = foundDate.format(dateFormatter)
        val priceColor = if (currentPrice < targetPrice) "#2ecc71" else "#e74c3c"

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
                    .price-box { background: $priceColor; color: white; padding: 15px; border-radius: 5px; text-align: center; margin: 15px 0; }
                    .route { font-size: 24px; font-weight: bold; }
                    .price { font-size: 32px; font-weight: bold; }
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
                        <p>Olá!</p>
                        <p>Encontramos uma passagem aérea correspondendo aos seus critérios de busca:</p>
                        
                        <div class="route" style="text-align: center; margin: 20px 0;">
                            $origin → $destination
                        </div>
                        
                        <div class="price-box">
                            <div class="label">Preço encontrado</div>
                            <div class="price">R$ $currentPrice</div>
                        </div>
                        
                        <table style="width: 100%; margin: 15px 0;">
                            <tr>
                                <td style="padding: 10px;"><strong>Preço-alvo:</strong></td>
                                <td style="padding: 10px; text-align: right;">R$ $targetPrice</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px;"><strong>Data da verificação:</strong></td>
                                <td style="padding: 10px; text-align: right;">$formattedDate</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px;"><strong>Economia:</strong></td>
                                <td style="padding: 10px; text-align: right; color: #2ecc71; font-weight: bold;">
                                    R$ ${"%.2f".format(targetPrice - currentPrice)}
                                </td>
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

