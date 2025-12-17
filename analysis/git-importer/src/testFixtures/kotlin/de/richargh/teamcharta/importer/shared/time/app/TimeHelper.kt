package de.richargh.teamcharta.importer.shared.time.app

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

fun String.zoned(): ZonedDateTime = ZonedDateTime.parse(this)

fun atStartOfYear(year: Int) = ZonedDateTime.of(
    LocalDate.of(year, 1, 1),
    LocalTime.of(0, 0),
    ZoneOffset.ofHours(1))

/** A fixed "now" for tests using 2025 dates - ensures all branches are active */
val testNow2025: ZonedDateTime = ZonedDateTime.of(
    LocalDate.of(2025, 2, 1),
    LocalTime.of(0, 0),
    ZoneOffset.ofHours(1))