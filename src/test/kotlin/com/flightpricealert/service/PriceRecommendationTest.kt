package com.flightpricealert.service

import com.flightpricealert.domain.PriceStats
import com.flightpricealert.domain.Recommendation
import kotlin.test.Test
import kotlin.test.assertEquals

class PriceRecommendationTest {
    private fun stats(count: Int, min: Double?, average: Double?) = PriceStats(count, min, average)

    @Test
    fun `returns none when there is not enough history yet`() {
        val result = PriceRecommendationEngine.evaluate(
            currentPrice = 100.0,
            stats = stats(count = 2, min = 90.0, average = 95.0),
            previousPrice = 95.0,
            targetPriceCeiling = null
        )
        assertEquals(Recommendation.NONE, result)
    }

    @Test
    fun `returns best price when at or below the historical minimum`() {
        val result = PriceRecommendationEngine.evaluate(
            currentPrice = 80.0,
            stats = stats(count = 5, min = 90.0, average = 100.0),
            previousPrice = 95.0,
            targetPriceCeiling = null
        )
        assertEquals(Recommendation.BEST_PRICE, result)
    }

    @Test
    fun `returns best price when exactly equal to the historical minimum`() {
        val result = PriceRecommendationEngine.evaluate(
            currentPrice = 90.0,
            stats = stats(count = 5, min = 90.0, average = 100.0),
            previousPrice = 95.0,
            targetPriceCeiling = null
        )
        assertEquals(Recommendation.BEST_PRICE, result)
    }

    @Test
    fun `returns good price when well below average but not the lowest ever`() {
        val result = PriceRecommendationEngine.evaluate(
            currentPrice = 85.0,
            stats = stats(count = 5, min = 80.0, average = 100.0),
            previousPrice = 95.0,
            targetPriceCeiling = null
        )
        assertEquals(Recommendation.GOOD_PRICE, result)
    }

    @Test
    fun `returns trending down when below average, falling, but not yet a good price`() {
        val result = PriceRecommendationEngine.evaluate(
            currentPrice = 96.0,
            stats = stats(count = 5, min = 80.0, average = 100.0),
            previousPrice = 98.0,
            targetPriceCeiling = null
        )
        assertEquals(Recommendation.TRENDING_DOWN, result)
    }

    @Test
    fun `returns none when price is at or above average`() {
        val result = PriceRecommendationEngine.evaluate(
            currentPrice = 100.0,
            stats = stats(count = 5, min = 80.0, average = 100.0),
            previousPrice = 105.0,
            targetPriceCeiling = null
        )
        assertEquals(Recommendation.NONE, result)
    }

    @Test
    fun `returns none when below average but rising compared to the previous check`() {
        val result = PriceRecommendationEngine.evaluate(
            currentPrice = 96.0,
            stats = stats(count = 5, min = 80.0, average = 100.0),
            previousPrice = 90.0,
            targetPriceCeiling = null
        )
        assertEquals(Recommendation.NONE, result)
    }

    @Test
    fun `never recommends above the optional target price ceiling`() {
        val result = PriceRecommendationEngine.evaluate(
            currentPrice = 80.0,
            stats = stats(count = 5, min = 80.0, average = 100.0),
            previousPrice = 95.0,
            targetPriceCeiling = 70.0
        )
        assertEquals(Recommendation.NONE, result)
    }

    @Test
    fun `ignores ceiling when it is not set`() {
        val result = PriceRecommendationEngine.evaluate(
            currentPrice = 80.0,
            stats = stats(count = 5, min = 80.0, average = 100.0),
            previousPrice = 95.0,
            targetPriceCeiling = null
        )
        assertEquals(Recommendation.BEST_PRICE, result)
    }
}
