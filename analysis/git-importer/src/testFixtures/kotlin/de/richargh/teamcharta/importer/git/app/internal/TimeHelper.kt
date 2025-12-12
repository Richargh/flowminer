package de.richargh.teamcharta.importer.git.app.internal

import java.time.ZonedDateTime

fun String.zoned(): ZonedDateTime = ZonedDateTime.parse(this)
