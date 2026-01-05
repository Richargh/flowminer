package de.richargh.teamcharta.importer.github.app.api

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Instant

class GitHubConnectionTest {

    @Test
    fun `GitHubConnection should hold token and use default baseUrl`() {
        // given
        val connection = GitHubCredentials(token = "ghp_xxx")

        // then
        connection.token shouldBe "ghp_xxx"
        connection.baseUrl shouldBe "https://api.github.com"
    }

    @Test
    fun `GitHubConnection should allow custom baseUrl for GitHub Enterprise`() {
        // given
        val connection = GitHubCredentials(
            token = "ghp_xxx",
            baseUrl = "https://github.mycompany.com/api/v3"
        )

        // then
        connection.baseUrl shouldBe "https://github.mycompany.com/api/v3"
    }
}

class ModelsTest {

    @Test
    fun `IssueNumber value should return wrapped int`() {
        // given
        val issueNumber = IssueNumber(123)

        // then
        issueNumber.value shouldBe 123
    }

    @Test
    fun `RepositoryId fullName should return owner slash name`() {
        // given
        val repoId = RepositoryId("octocat", "hello-world")

        // then
        repoId.fullName shouldBe "octocat/hello-world"
    }

    @Test
    fun `WorkItemState Open name should return Open`() {
        // then
        WorkItemState.Open.name shouldBe "Open"
        WorkItemState.Closed.name shouldBe "Closed"
    }

    @Test
    fun `WorkItemType values should include all six types`() {
        // then
        val types = WorkItemType.entries
        types.map { it.name } shouldBe listOf("Issue", "Epic", "Feature", "Story", "Task", "Bug")
    }

    @Test
    fun `StateTransition should hold field from to at actor`() {
        // given
        val instant = Instant.parse("2024-01-15T10:00:00Z")
        val transition = StateTransition(
            field = TransitionField.State,
            from = "open",
            to = "closed",
            at = instant,
            actor = "octocat"
        )

        // then
        transition.field shouldBe TransitionField.State
        transition.from shouldBe "open"
        transition.to shouldBe "closed"
        transition.at shouldBe instant
        transition.actor shouldBe "octocat"
    }

    @Test
    fun `GitHubWorkItem should hold all issue properties`() {
        // given
        val created = Instant.parse("2024-01-01T10:00:00Z")
        val closed = Instant.parse("2024-01-15T10:00:00Z")
        val transition = StateTransition(
            field = TransitionField.State,
            from = "open",
            to = "closed",
            at = closed,
            actor = "octocat"
        )

        val parentRepo = RepositoryId("octocat", "hello-world")
        val workItem = GitHubWorkItem(
            id = IssueNumber(123),
            title = "Fix bug",
            state = WorkItemState.Closed,
            type = WorkItemType.Bug,
            labels = listOf("bug", "priority:high"),
            assignees = listOf("octocat", "hubot"),
            milestone = "v1.0",
            parent = IssueReference(parentRepo, IssueNumber(100)),
            childIds = listOf(IssueNumber(124), IssueNumber(125)),
            created = created,
            closed = closed,
            transitions = listOf(transition)
        )

        // then
        workItem.id.value shouldBe 123
        workItem.title shouldBe "Fix bug"
        workItem.state shouldBe WorkItemState.Closed
        workItem.type shouldBe WorkItemType.Bug
        workItem.labels shouldBe listOf("bug", "priority:high")
        workItem.assignees shouldBe listOf("octocat", "hubot")
        workItem.milestone shouldBe "v1.0"
        workItem.parent?.number?.value shouldBe 100
        workItem.childIds.map { it.value } shouldBe listOf(124, 125)
        workItem.created shouldBe created
        workItem.closed shouldBe closed
        workItem.transitions shouldBe listOf(transition)
    }
}
