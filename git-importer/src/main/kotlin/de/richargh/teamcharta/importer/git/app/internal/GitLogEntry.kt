package de.richargh.teamcharta.importer.git.app.internal

import java.time.Instant

/**
 * Internal representation of a single git log entry.
 * Used during parsing before converting to domain events.
 */
data class GitLogEntry(
    val hash: String,
    val parents: List<String>,
    val author: String,
    val timestamp: Instant,
    val refs: List<String>,
    val message: String,
    val filesChanged: Int = 0
)
