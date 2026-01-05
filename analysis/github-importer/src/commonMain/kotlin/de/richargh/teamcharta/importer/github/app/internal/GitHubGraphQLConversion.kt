package de.richargh.teamcharta.importer.github.app.internal

import de.richargh.teamcharta.importer.github.app.api.GitHubWorkItem
import de.richargh.teamcharta.importer.github.app.api.IssueNumber
import de.richargh.teamcharta.importer.github.app.api.IssueReference
import de.richargh.teamcharta.importer.github.app.api.RepositoryId
import de.richargh.teamcharta.importer.github.app.api.StateTransition
import de.richargh.teamcharta.importer.github.app.api.TransitionField
import de.richargh.teamcharta.importer.github.app.api.WorkItemState
import de.richargh.teamcharta.importer.github.app.api.WorkItemType
import kotlin.time.Instant

/**
 * Converts a GraphQL Issue DTO to a domain GitHubWorkItem.
 * Parent references include full repository info (supports cross-repo parents).
 */
fun GraphQLIssue.toGitHubWorkItem(repoId: RepositoryId): GitHubWorkItem = GitHubWorkItem(
    id = IssueNumber(number),
    title = title,
    state = convertState(state),
    type = inferWorkItemTypeFromGraphQL(labels?.nodes ?: emptyList()),
    labels = labels?.nodes?.map { it.name } ?: emptyList(),
    assignees = assignees?.nodes?.map { it.login } ?: emptyList(),
    milestone = milestone?.title,
    parent = parent?.toIssueReference(repoId),
    childIds = subIssues?.nodes?.map { IssueNumber(it.number) } ?: emptyList(),
    created = Instant.parse(createdAt),
    closed = closedAt?.let { Instant.parse(it) },
    transitions = timelineItems?.nodes?.mapNotNull { it.toStateTransition() } ?: emptyList()
)

/**
 * Converts a GraphQL issue reference to domain IssueReference.
 * If repository info is missing, assumes the current repository.
 */
private fun GraphQLIssueReference.toIssueReference(currentRepo: RepositoryId): IssueReference {
    val repo = repository?.let { RepositoryId(it.owner.login, it.name) } ?: currentRepo
    return IssueReference(repo, IssueNumber(number))
}

/**
 * Converts GraphQL state string to WorkItemState enum.
 * GraphQL uses uppercase: "OPEN" and "CLOSED"
 */
private fun convertState(state: String): WorkItemState = when (state) {
    "OPEN" -> WorkItemState.Open
    "CLOSED" -> WorkItemState.Closed
    else -> throw IllegalArgumentException("Unknown GraphQL issue state: $state")
}

/**
 * Infers the work item type from GraphQL labels.
 * Checks for "type:" prefixed labels first, then exact matches.
 */
private fun inferWorkItemTypeFromGraphQL(labels: List<GraphQLLabel>): WorkItemType? {
    for (label in labels) {
        val name = label.name.lowercase()

        // Check for "type:" prefixed labels
        if (name.startsWith("type:")) {
            val typeName = name.removePrefix("type:")
            when (typeName) {
                "epic" -> return WorkItemType.Epic
                "feature" -> return WorkItemType.Feature
                "story" -> return WorkItemType.Story
                "task" -> return WorkItemType.Task
                "bug" -> return WorkItemType.Bug
                "issue" -> return WorkItemType.Issue
            }
        }

        // Check for exact matches
        when (name) {
            "epic" -> return WorkItemType.Epic
            "feature" -> return WorkItemType.Feature
            "story" -> return WorkItemType.Story
            "task" -> return WorkItemType.Task
            "bug" -> return WorkItemType.Bug
        }
    }
    return null
}

/**
 * Converts a GraphQL Timeline Event to a domain StateTransition.
 * Returns null for event types that don't represent state transitions.
 */
fun GraphQLTimelineItem.toStateTransition(): StateTransition? {
    return when (this) {
        is GraphQLTimelineItem.LabeledEvent -> StateTransition(
            field = TransitionField.Label,
            from = null,
            to = label.name,
            at = Instant.parse(createdAt),
            actor = actor?.login
        )
        is GraphQLTimelineItem.UnlabeledEvent -> StateTransition(
            field = TransitionField.Label,
            from = label.name,
            to = null,
            at = Instant.parse(createdAt),
            actor = actor?.login
        )
        is GraphQLTimelineItem.AssignedEvent -> StateTransition(
            field = TransitionField.Assignee,
            from = null,
            to = assignee?.login,
            at = Instant.parse(createdAt),
            actor = actor?.login
        )
        is GraphQLTimelineItem.UnassignedEvent -> StateTransition(
            field = TransitionField.Assignee,
            from = assignee?.login,
            to = null,
            at = Instant.parse(createdAt),
            actor = actor?.login
        )
        is GraphQLTimelineItem.ClosedEvent -> StateTransition(
            field = TransitionField.State,
            from = "open",
            to = "closed",
            at = Instant.parse(createdAt),
            actor = actor?.login
        )
        is GraphQLTimelineItem.ReopenedEvent -> StateTransition(
            field = TransitionField.State,
            from = "closed",
            to = "open",
            at = Instant.parse(createdAt),
            actor = actor?.login
        )
        is GraphQLTimelineItem.MilestonedEvent -> StateTransition(
            field = TransitionField.Milestone,
            from = null,
            to = milestoneTitle,
            at = Instant.parse(createdAt),
            actor = actor?.login
        )
        is GraphQLTimelineItem.DemilestonedEvent -> StateTransition(
            field = TransitionField.Milestone,
            from = milestoneTitle,
            to = null,
            at = Instant.parse(createdAt),
            actor = actor?.login
        )
        is GraphQLTimelineItem.Other -> null
    }
}
