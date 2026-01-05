package de.richargh.teamcharta.importer.githubfixtures

import de.richargh.teamcharta.importer.github.app.internal.*

// Simple fixture functions for GraphQL DTOs

fun aGraphQLPageInfo(
    hasNextPage: Boolean = false,
    endCursor: String? = null
) = GraphQLPageInfo(hasNextPage, endCursor)

fun aGraphQLAuthor(login: String = "octocat") = GraphQLAuthor(login)

fun aGraphQLLabel(
    name: String = "bug",
    color: String? = "d73a4a"
) = GraphQLLabel(name, color)

fun aGraphQLMilestone(
    title: String = "v1.0",
    state: String = "OPEN"
) = GraphQLMilestone(title, state)

fun aGraphQLIssueReference(
    number: Int = 1,
    owner: String? = null,
    repoName: String? = null
) = GraphQLIssueReference(
    number = number,
    repository = if (owner != null && repoName != null) {
        GraphQLRepositoryRef(GraphQLRepositoryOwner(owner), repoName)
    } else null
)

// GraphQL Issue Builder

class GraphQLIssueBuilder {
    private var number: Int = 1
    private var title: String = "Test issue"
    private var state: String = "OPEN"
    private var body: String? = null
    private var createdAt: String = "2024-01-01T10:00:00Z"
    private var closedAt: String? = null
    private var author: GraphQLAuthor? = null
    private var labels: GraphQLConnection<GraphQLLabel>? = null
    private var assignees: GraphQLConnection<GraphQLAuthor>? = null
    private var milestone: GraphQLMilestone? = null
    private var parent: GraphQLIssueReference? = null
    private var subIssues: GraphQLConnection<GraphQLIssueReference>? = null
    private var timelineItems: GraphQLConnection<GraphQLTimelineItem>? = null

    fun number(number: Int) = apply { this.number = number }
    fun title(title: String) = apply { this.title = title }
    fun state(state: String) = apply { this.state = state }
    fun body(body: String?) = apply { this.body = body }
    fun createdAt(createdAt: String) = apply { this.createdAt = createdAt }
    fun closedAt(closedAt: String?) = apply { this.closedAt = closedAt }
    fun author(author: GraphQLAuthor?) = apply { this.author = author }
    fun author(login: String) = apply { this.author = GraphQLAuthor(login) }
    fun labels(vararg labels: GraphQLLabel) = apply {
        this.labels = GraphQLConnection(labels.toList())
    }
    fun assignees(vararg assignees: GraphQLAuthor) = apply {
        this.assignees = GraphQLConnection(assignees.toList())
    }
    fun milestone(milestone: GraphQLMilestone?) = apply { this.milestone = milestone }
    fun milestone(title: String) = apply { this.milestone = GraphQLMilestone(title, "OPEN") }
    fun parent(parent: GraphQLIssueReference?) = apply { this.parent = parent }
    fun parent(number: Int) = apply { this.parent = GraphQLIssueReference(number) }
    fun parent(number: Int, owner: String, repoName: String) = apply {
        this.parent = aGraphQLIssueReference(number, owner, repoName)
    }
    fun subIssues(vararg refs: GraphQLIssueReference) = apply {
        this.subIssues = GraphQLConnection(refs.toList())
    }
    fun timelineItems(vararg items: GraphQLTimelineItem) = apply {
        this.timelineItems = GraphQLConnection(items.toList())
    }

    fun build(): GraphQLIssue = GraphQLIssue(
        number = number,
        title = title,
        state = state,
        body = body,
        createdAt = createdAt,
        closedAt = closedAt,
        author = author,
        labels = labels,
        assignees = assignees,
        milestone = milestone,
        parent = parent,
        subIssues = subIssues,
        timelineItems = timelineItems
    )
}

fun aGraphQLIssue(block: GraphQLIssueBuilder.() -> Unit = {}): GraphQLIssue =
    GraphQLIssueBuilder().apply(block).build()

// Timeline Event Builders

fun aGraphQLLabeledEvent(
    createdAt: String = "2024-01-15T10:00:00Z",
    actor: GraphQLAuthor? = aGraphQLAuthor(),
    label: GraphQLLabel = aGraphQLLabel()
) = GraphQLTimelineItem.LabeledEvent(createdAt, actor, label)

fun aGraphQLUnlabeledEvent(
    createdAt: String = "2024-01-15T10:00:00Z",
    actor: GraphQLAuthor? = aGraphQLAuthor(),
    label: GraphQLLabel = aGraphQLLabel()
) = GraphQLTimelineItem.UnlabeledEvent(createdAt, actor, label)

fun aGraphQLAssignedEvent(
    createdAt: String = "2024-01-15T10:00:00Z",
    actor: GraphQLAuthor? = aGraphQLAuthor(),
    assignee: GraphQLAuthor? = aGraphQLAuthor("dev")
) = GraphQLTimelineItem.AssignedEvent(createdAt, actor, assignee)

fun aGraphQLUnassignedEvent(
    createdAt: String = "2024-01-15T10:00:00Z",
    actor: GraphQLAuthor? = aGraphQLAuthor(),
    assignee: GraphQLAuthor? = aGraphQLAuthor("dev")
) = GraphQLTimelineItem.UnassignedEvent(createdAt, actor, assignee)

fun aGraphQLClosedEvent(
    createdAt: String = "2024-01-15T10:00:00Z",
    actor: GraphQLAuthor? = aGraphQLAuthor()
) = GraphQLTimelineItem.ClosedEvent(createdAt, actor)

fun aGraphQLReopenedEvent(
    createdAt: String = "2024-01-15T10:00:00Z",
    actor: GraphQLAuthor? = aGraphQLAuthor()
) = GraphQLTimelineItem.ReopenedEvent(createdAt, actor)

fun aGraphQLMilestonedEvent(
    createdAt: String = "2024-01-15T10:00:00Z",
    actor: GraphQLAuthor? = aGraphQLAuthor(),
    milestoneTitle: String = "v1.0"
) = GraphQLTimelineItem.MilestonedEvent(createdAt, actor, milestoneTitle)

fun aGraphQLDemilestonedEvent(
    createdAt: String = "2024-01-15T10:00:00Z",
    actor: GraphQLAuthor? = aGraphQLAuthor(),
    milestoneTitle: String = "v1.0"
) = GraphQLTimelineItem.DemilestonedEvent(createdAt, actor, milestoneTitle)

// GraphQL Response Builder

class GraphQLResponseBuilder {
    private var issues: MutableList<GraphQLIssue> = mutableListOf()
    private var pageInfo: GraphQLPageInfo = GraphQLPageInfo(false, null)
    private var errors: List<GraphQLError>? = null

    fun issues(vararg issues: GraphQLIssue) = apply { this.issues = issues.toMutableList() }
    fun pageInfo(pageInfo: GraphQLPageInfo) = apply { this.pageInfo = pageInfo }
    fun hasNextPage(hasNextPage: Boolean, endCursor: String? = null) = apply {
        this.pageInfo = GraphQLPageInfo(hasNextPage, endCursor)
    }
    fun errors(vararg errors: GraphQLError) = apply { this.errors = errors.toList() }

    fun build(): GraphQLResponse = GraphQLResponse(
        data = if (errors == null) GraphQLData(
            repository = GraphQLRepository(
                issues = GraphQLIssueConnection(
                    nodes = issues,
                    pageInfo = pageInfo
                )
            )
        ) else null,
        errors = errors
    )
}

fun aGraphQLResponse(block: GraphQLResponseBuilder.() -> Unit = {}): GraphQLResponse =
    GraphQLResponseBuilder().apply(block).build()

fun aGraphQLError(
    message: String = "Error occurred",
    type: String? = null
) = GraphQLError(message, type)
