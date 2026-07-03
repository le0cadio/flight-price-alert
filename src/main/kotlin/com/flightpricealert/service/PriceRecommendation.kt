package com.flightpricealert.service

import com.flightpricealert.domain.PriceStats
import com.flightpricealert.domain.Recommendation

object PriceRecommendationEngine {
    const val DEFAULT_MIN_SAMPLES = 3
    const val DEFAULT_BELOW_AVERAGE_PERCENT = 0.10

    /**
     * [stats] must reflect the history BEFORE [currentPrice] was recorded, so the current
     * reading is always compared against a baseline it wasn't part of.
     */
    fun evaluate(
        currentPrice: Double,
        stats: PriceStats,
        previousPrice: Double?,
        targetPriceCeiling: Double?,
        minSamples: Int = DEFAULT_MIN_SAMPLES,
        belowAveragePercent: Double = DEFAULT_BELOW_AVERAGE_PERCENT
    ): Recommendation {
        if (targetPriceCeiling != null && currentPrice > targetPriceCeiling) {
            return Recommendation.NONE
        }

        val average = stats.average
        val min = stats.min
        if (stats.count < minSamples || average == null || min == null) {
            return Recommendation.NONE
        }

        if (currentPrice <= min) {
            return Recommendation.BEST_PRICE
        }

        val goodPriceThreshold = average * (1 - belowAveragePercent)
        if (currentPrice <= goodPriceThreshold) {
            return Recommendation.GOOD_PRICE
        }

        if (currentPrice < average && previousPrice != null && currentPrice < previousPrice) {
            return Recommendation.TRENDING_DOWN
        }

        return Recommendation.NONE
    }
}
