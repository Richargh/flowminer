package de.richargh.teamcharta.importer.githubfixtures

import de.richargh.teamcharta.importer.github.app.api.TransitionField
import de.richargh.teamcharta.importer.github.app.api.WorkItemState
import de.richargh.teamcharta.importer.github.app.api.WorkItemType
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Instant

class GitHubWorkItemBuilderTest {

    @Test
    fun `aGitHubWorkItem should create GitHubWorkItem with specified values`() {
        // given
        val created = Instant.parse("2024-01-01T10:00:00Z")
        val closed = Instant.parse("2024-01-15T10:00:00Z")
        val transition = aStateTransition {
            field(TransitionField.State)
            from("open")
            to("closed")
            at(closed)
            actor("octocat")
        }

        // when
        val workItem = aGitHubWorkItem {
            id(123)
            title("Fix bug")
            state(WorkItemState.Closed)
            type(WorkItemType.Bug)
            labels("bug", "priority:high")
            assignees("octocat", "hubot")
            milestone("v1.0")
            parent(100)
            childIds(124, 125)
            created(created)
            closed(closed)
            transitions(transition)
        }

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

    @Test
    fun `aGitHubWorkItem should use default values when not specified`() {
        // when
        val workItem = aGitHubWorkItem()

        // then
        workItem.id.value shouldBe 1
        workItem.title shouldBe "Test issue"
        workItem.state shouldBe WorkItemState.Open
        workItem.type shouldBe null
        workItem.labels shouldBe emptyList()
        workItem.assignees shouldBe emptyList()
        workItem.milestone shouldBe null
        workItem.parent shouldBe null
        workItem.childIds shouldBe emptyList()
        workItem.closed shouldBe null
        workItem.transitions shouldBe emptyList()
    }
}
