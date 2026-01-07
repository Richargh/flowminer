package de.richargh.flowminer.importer.githubfixtures

import de.richargh.flowminer.importer.github.app.api.GitHubWorkItem
import de.richargh.flowminer.importer.github.app.api.IssueNumber
import de.richargh.flowminer.importer.github.app.api.IssueReference
import de.richargh.flowminer.importer.github.app.api.RepositoryId
import de.richargh.flowminer.importer.github.app.api.StateTransition
import de.richargh.flowminer.importer.github.app.api.WorkItemState
import de.richargh.flowminer.importer.github.app.api.WorkItemType
import kotlin.time.Instant

/** Default test repository for builders */
val testRepoId = RepositoryId(owner = "test-owner", name = "test-repo")

class GitHubWorkItemBuilder {
    private var id: IssueNumber = IssueNumber(1)
    private var title: String = "Test issue"
    private var state: WorkItemState = WorkItemState.Open
    private var type: WorkItemType? = null
    private var labels: List<String> = emptyList()
    private var assignees: List<String> = emptyList()
    private var milestone: String? = null
    private var parent: IssueReference? = null
    private var childIds: List<IssueNumber> = emptyList()
    private var created: Instant = Instant.parse("2024-01-01T10:00:00Z")
    private var closed: Instant? = null
    private var transitions: List<StateTransition> = emptyList()

    fun id(id: Int) = apply { this.id = IssueNumber(id) }
    fun id(id: IssueNumber) = apply { this.id = id }
    fun title(title: String) = apply { this.title = title }
    fun state(state: WorkItemState) = apply { this.state = state }
    fun type(type: WorkItemType?) = apply { this.type = type }
    fun labels(vararg labels: String) = apply { this.labels = labels.toList() }
    fun assignees(vararg assignees: String) = apply { this.assignees = assignees.toList() }
    fun milestone(milestone: String?) = apply { this.milestone = milestone }
    /** Set parent (assumes same repo as testRepoId) */
    fun parent(parentNumber: Int?) = apply {
        this.parent = parentNumber?.let { IssueReference(testRepoId, IssueNumber(it)) }
    }
    /** Set parent with full reference */
    fun parent(parent: IssueReference?) = apply { this.parent = parent }
    /** Set parent with explicit repo */
    fun parent(parentNumber: Int, owner: String, repoName: String) = apply {
        this.parent = IssueReference(RepositoryId(owner, repoName), IssueNumber(parentNumber))
    }
    fun childIds(vararg childIds: Int) = apply { this.childIds = childIds.map { IssueNumber(it) } }
    fun created(created: Instant) = apply { this.created = created }
    fun closed(closed: Instant?) = apply { this.closed = closed }
    fun transitions(vararg transitions: StateTransition) = apply { this.transitions = transitions.toList() }
    fun transitions(transitions: List<StateTransition>) = apply { this.transitions = transitions }

    fun build(): GitHubWorkItem = GitHubWorkItem(
        id = id,
        title = title,
        state = state,
        type = type,
        labels = labels,
        assignees = assignees,
        milestone = milestone,
        parent = parent,
        childIds = childIds,
        created = created,
        closed = closed,
        transitions = transitions
    )
}

fun aGitHubWorkItem(block: GitHubWorkItemBuilder.() -> Unit = {}): GitHubWorkItem =
    GitHubWorkItemBuilder().apply(block).build()
