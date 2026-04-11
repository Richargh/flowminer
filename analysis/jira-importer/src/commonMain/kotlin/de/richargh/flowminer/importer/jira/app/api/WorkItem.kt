package de.richargh.flowminer.importer.jira.app.api

import kotlin.time.Instant

data class IssueKey(val value: String)

data class WorkItem(
    val key: IssueKey,
    val name: String?,
    val type: String?,
    val state: String?,
    val started: Instant?,
    val finished: Instant?,
    val transitions: List<StateTransition>
)

data class StateTransition(
    val from: String?,
    val to: String?,
    val at: Instant?
)
