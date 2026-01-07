package de.richargh.flowminer.shared.calculations

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
object NormalizationCalculations {

    /**
     * Calculates the percentile of a value within a given range.
     *
     * @param value The value to calculate the percentile for
     * @param min The minimum value in the range
     * @param max The maximum value in the range
     * @return The percentile (0-100), or 0 if min equals max
     */
    fun calculatePercentile(value: Double, min: Double, max: Double): Double {
        if (max == min) return 0.0
        return ((value - min) / (max - min)) * 100.0
    }
}
