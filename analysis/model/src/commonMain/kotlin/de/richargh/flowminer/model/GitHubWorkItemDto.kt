package de.richargh.flowminer.model

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
interface GitHubWorkItemDto {
    val id: Int
    val title: String
    val state: String  // "Open" or "Closed"
    val type: String?  // "Issue", "Epic", "Feature", "Story", "Task", "Bug", or null
    val labels: List<String>
    val assignees: List<String>
    val milestone: String?
    val parentId: Int?
    val childIds: List<Int>
    val created: String  // ISO-8601 format
    val closed: String?  // ISO-8601 format, null if not closed
    val transitions: List<StateTransitionDto>
}

@Serializable
data class SerializableGitHubWorkItemDto(
    override val id: Int,
    override val title: String,
    override val state: String,
    override val type: String?,
    override val labels: List<String>,
    override val assignees: List<String>,
    override val milestone: String?,
    override val parentId: Int?,
    override val childIds: List<Int>,
    override val created: String,
    override val closed: String?,
    override val transitions: List<SerializableStateTransitionDto>
) : GitHubWorkItemDto
