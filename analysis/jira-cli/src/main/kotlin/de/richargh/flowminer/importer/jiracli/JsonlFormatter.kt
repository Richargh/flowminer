package de.richargh.flowminer.importer.jiracli

import de.richargh.flowminer.importer.jira.app.api.StateTransition
import de.richargh.flowminer.importer.jira.app.api.WorkItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

fun writeWorkItemsAsJsonl(items: List<WorkItem>, outputFile: File) {
    val json = Json { encodeDefaults = true }
    val lines = items.map { item -> json.encodeToString(item.toJsonlDto()) }
    outputFile.writeText(lines.joinToString("\n", postfix = "\n"))
}

@Serializable
private data class WorkItemJsonlDto(
    val key: String,
    val name: String?,
    val type: String?,
    val state: String?,
    val started: String?,
    val finished: String?,
    val transitions: List<StateTransitionJsonlDto>
)

@Serializable
private data class StateTransitionJsonlDto(
    val from: String?,
    val to: String?,
    val at: String?
)

private fun WorkItem.toJsonlDto() = WorkItemJsonlDto(
    key = key.value,
    name = name,
    type = type,
    state = state,
    started = started?.toString(),
    finished = finished?.toString(),
    transitions = transitions.map { it.toJsonlDto() }
)

private fun StateTransition.toJsonlDto() = StateTransitionJsonlDto(
    from = from,
    to = to,
    at = at?.toString()
)
