package de.richargh.flowminer.importer.github.app.api

import kotlin.time.Instant

/**
 * Semantic wrapper for GitHub issue numbers.
 */
data class IssueNumber(val value: Int)

/**
 * Semantic wrapper for GitHub repository identification.
 */
data class RepositoryId(val owner: String, val name: String) {
    val fullName: String get() = "$owner/$name"
}

/**
 * Reference to an issue in a specific repository.
 * Used for cross-repo parent references.
 */
data class IssueReference(
    val repo: RepositoryId,
    val number: IssueNumber
) {
    /** Returns true if this reference points to an issue in the given repository */
    fun isInRepo(repoId: RepositoryId): Boolean =
        repo.owner == repoId.owner && repo.name == repoId.name
}

/**
 * State of a work item (issue).
 */
enum class WorkItemState {
    Open,
    Closed
}

/**
 * Type of work item, inferred from labels.
 */
enum class WorkItemType {
    Issue,
    Epic,
    Feature,
    Story,
    Task,
    Bug
}

/**
 * The field that changed in a state transition.
 */
enum class TransitionField {
    Label,
    Assignee,
    State,
    Milestone
    // TODO other is also possible
}

/**
 * Represents a state change in an issue's lifecycle.
 */
data class StateTransition(
    val field: TransitionField,
    val from: String?,
    val to: String?,
    val at: Instant,
    val actor: String?
)

/**
 * Domain model for a GitHub issue as a work item.
 */
data class GitHubWorkItem(
    val id: IssueNumber,
    val title: String,
    val state: WorkItemState,
    val type: WorkItemType?,
    val labels: List<String>,
    val assignees: List<String>,
    val milestone: String?,
    val parent: IssueReference?,
    val childIds: List<IssueNumber>,
    val created: Instant,
    val closed: Instant?,
    val transitions: List<StateTransition>
)
