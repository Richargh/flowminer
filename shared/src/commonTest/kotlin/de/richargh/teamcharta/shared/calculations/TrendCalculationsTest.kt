package de.richargh.teamcharta.shared.calculations

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrendCalculationsTest {

    private fun assertClose(expected: Double, actual: Double, tolerance: Double = 0.001) {
        assertTrue(
            abs(expected - actual) < tolerance,
            "Expected $expected but got $actual (tolerance: $tolerance)"
        )
    }

    @Test
    fun linear_regression_calculates_slope_and_intercept_for_perfect_line() {
        // y = 2x + 1
        val xValues = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val yValues = listOf(3.0, 5.0, 7.0, 9.0, 11.0)

        val result = TrendCalculations.linearRegression(xValues, yValues)

        assertClose(2.0, result.slope)
        assertClose(1.0, result.intercept)
    }

    @Test
    fun linear_regression_handles_horizontal_line() {
        // y = 5
        val xValues = listOf(1.0, 2.0, 3.0, 4.0)
        val yValues = listOf(5.0, 5.0, 5.0, 5.0)

        val result = TrendCalculations.linearRegression(xValues, yValues)

        assertClose(0.0, result.slope)
        assertClose(5.0, result.intercept)
    }

    @Test
    fun linear_regression_handles_negative_slope() {
        // y = -x + 10
        val xValues = listOf(0.0, 2.0, 4.0, 6.0)
        val yValues = listOf(10.0, 8.0, 6.0, 4.0)

        val result = TrendCalculations.linearRegression(xValues, yValues)

        assertClose(-1.0, result.slope)
        assertClose(10.0, result.intercept)
    }

    @Test
    fun linear_regression_handles_single_point() {
        val xValues = listOf(5.0)
        val yValues = listOf(10.0)

        val result = TrendCalculations.linearRegression(xValues, yValues)

        assertEquals(0.0, result.slope)
        assertEquals(10.0, result.intercept)
    }

    @Test
    fun linear_regression_handles_empty_lists() {
        val result = TrendCalculations.linearRegression(emptyList(), emptyList())

        assertEquals(0.0, result.slope)
        assertEquals(0.0, result.intercept)
    }

    @Test
    fun linear_regression_handles_scattered_data() {
        // Approximate best fit line
        val xValues = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val yValues = listOf(2.1, 4.2, 5.8, 8.1, 9.9)

        val result = TrendCalculations.linearRegression(xValues, yValues)

        // Should approximate y ≈ 2x
        assertTrue(result.slope > 1.5 && result.slope < 2.5)
    }
}
