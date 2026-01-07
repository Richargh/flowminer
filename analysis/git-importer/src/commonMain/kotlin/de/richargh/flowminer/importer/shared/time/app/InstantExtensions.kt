package de.richargh.flowminer.importer.shared.time.app

import kotlin.time.Duration
import kotlin.time.Instant

/** Format instant as ISO date (yyyy-MM-dd) */
fun Instant.toIsoDateString(): String = toString().substringBefore('T')

/** Format duration as human-readable relative time */
fun Duration.toRelativeString(): String {
    val days = inWholeDays
    return when {
        days >= 365 -> "${days / 365} years"
        days >= 30 -> "${days / 30} months"
        days >= 7 -> "${days / 7} weeks"
        else -> "$days days"
    }
}
