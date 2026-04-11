package de.richargh.flowminer.importer.jira.app.dto

import de.richargh.flowminer.importer.jira.app.api.IssueKey
import de.richargh.flowminer.importer.jira.app.api.StateTransition
import de.richargh.flowminer.importer.jira.app.api.WorkItem
import kotlin.time.Instant

private val startedStatuses = setOf("In Progress")
private val finishedStatuses = setOf("Done", "Closed")

fun JiraApiIssueDto.toWorkItem(): WorkItem {
    requireNotNull(key) { "Jira issue key must not be null" }

    var started: Instant? = null
    var finished: Instant? = null
    val transitions = mutableListOf<StateTransition>()

    for (history in changelog?.histories ?: emptyList()) {
        val at = history.created?.let { parseJiraInstant(it) }
        for (item in history.items) {
            if (item.field == "status") {
                transitions.add(StateTransition(from = item.fromString, to = item.toStringValue, at = at))
                if (item.toStringValue in startedStatuses && started == null) {
                    started = at
                }
                if (item.toStringValue in finishedStatuses && finished == null) {
                    finished = at
                }
            }
        }
    }

    return WorkItem(
        key = IssueKey(key),
        name = fields?.summary,
        type = fields?.issuetype?.name,
        state = fields?.status?.name,
        started = started,
        finished = finished,
        transitions = transitions
    )
}

internal fun parseJiraInstant(dateStr: String): Instant {
    // Jira returns dates like "2024-06-01T10:00:00.000+0000"
    // Normalize "+0000" → "+00:00" and similar offsets for ISO-8601 parsing
    val normalized = dateStr.replace(Regex("""([+-]\d{2})(\d{2})$"""), "$1:$2")
    return try {
        Instant.parse(normalized)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Failed to parse Jira date: '$dateStr'", e)
    }
}
