package de.richargh.flowminer.shared.calculations

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class LinearRegressionResult(
    val slope: Double,
    val intercept: Double
)

@OptIn(ExperimentalJsExport::class)
@JsExport
object TrendCalculations {

    /**
     * Calculates linear regression (least squares fit) for the given data points.
     *
     * @param xValues The x coordinates of data points
     * @param yValues The y coordinates of data points
     * @return LinearRegressionResult containing slope and intercept (y = slope * x + intercept)
     */
    fun linearRegression(xValues: List<Double>, yValues: List<Double>): LinearRegressionResult {
        if (xValues.isEmpty() || yValues.isEmpty()) {
            return LinearRegressionResult(
                slope = 0.0,
                intercept = 0.0
            )
        }

        val n = xValues.size.toDouble()

        if (n == 1.0) {
            return LinearRegressionResult(
                slope = 0.0,
                intercept = yValues[0]
            )
        }

        val sumX = xValues.sum()
        val sumY = yValues.sum()
        val sumXY = xValues.zip(yValues).sumOf { (x, y) -> x * y }
        val sumXX = xValues.sumOf { it * it }

        val denominator = n * sumXX - sumX * sumX

        if (denominator == 0.0) {
            return LinearRegressionResult(
                slope = 0.0,
                intercept = sumY / n
            )
        }

        val slope = (n * sumXY - sumX * sumY) / denominator
        val intercept = (sumY - slope * sumX) / n

        return LinearRegressionResult(
            slope = slope,
            intercept = intercept
        )
    }
}
