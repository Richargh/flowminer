package de.richargh.teamcharta.shared.calculations

import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizationCalculationsTest {

    @Test
    fun calculate_percentile_returns_0_for_value_equal_to_min() {
        val result = NormalizationCalculations.calculatePercentile(
            value = 10.0,
            min = 10.0,
            max = 100.0
        )
        assertEquals(0.0, result)
    }

    @Test
    fun calculate_percentile_returns_100_for_value_equal_to_max() {
        val result = NormalizationCalculations.calculatePercentile(
            value = 100.0,
            min = 10.0,
            max = 100.0
        )
        assertEquals(100.0, result)
    }

    @Test
    fun calculate_percentile_returns_50_for_midpoint_value() {
        val result = NormalizationCalculations.calculatePercentile(
            value = 55.0,
            min = 10.0,
            max = 100.0
        )
        assertEquals(50.0, result)
    }

    @Test
    fun calculate_percentile_returns_0_when_min_equals_max() {
        val result = NormalizationCalculations.calculatePercentile(
            value = 50.0,
            min = 50.0,
            max = 50.0
        )
        assertEquals(0.0, result)
    }

    @Test
    fun calculate_percentile_handles_decimal_values() {
        val result = NormalizationCalculations.calculatePercentile(
            value = 3.5,
            min = 1.0,
            max = 6.0
        )
        assertEquals(50.0, result)
    }
}
