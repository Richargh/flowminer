package de.richargh.teamcharta.importer.jira.app.api

import java.time.OffsetDateTime

data class WorkItem(
    val key: String,
    val name: String?,
    val type: String?,
    val state: String?,
    val started: OffsetDateTime?,
    val finished: OffsetDateTime?,
    val transition: List<StateTransition>
)

data class StateTransition(
    val from: String?,
    val to: String?,
    val at: OffsetDateTime?
)