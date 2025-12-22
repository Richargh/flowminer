package de.richargh.teamcharta.importer.shared.time.app

import kotlin.time.Instant

fun String.toInstant(): Instant = Instant.parse(this)

fun atStartOfYear(year: Int): Instant = Instant.parse("$year-01-01T00:00:00+01:00")

/** A fixed "now" for tests using 2025 dates - ensures all branches are active */
val testNow2025: Instant = Instant.parse("2025-02-01T00:00:00+01:00")