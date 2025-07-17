package de.richargh.teamcharta.importer.jira.app.api

import java.time.OffsetDateTime

data class JiraIssue(
    val key: String,
    val name: String?,
    val type: String?,
    val state: String?,
    val started: OffsetDateTime?,
    val finished: OffsetDateTime?,
    val cycleTime: Long?
)