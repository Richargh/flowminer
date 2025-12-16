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