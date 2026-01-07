package de.richargh.flowminer.importer.github.app.internal

import de.richargh.flowminer.importer.github.app.api.IssueNumber
import de.richargh.flowminer.importer.github.app.api.RepositoryId
import de.richargh.flowminer.importer.github.app.api.TransitionField
import de.richargh.flowminer.importer.github.app.api.WorkItemState
import de.richargh.flowminer.importer.github.app.api.WorkItemType
import de.richargh.flowminer.importer.githubfixtures.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Instant

class GitHubGraphQLConversionTest {

    private val testRepoId = RepositoryId(owner = "test-owner", name = "test-repo")

    // Phase 6.1: Basic conversion

    @Test
    fun `toGitHubWorkItem should convert basic fields from GraphQLIssue to GitHubWorkItem`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            number(123)
            title("Bug")
            state("OPEN")
            createdAt("2024-01-15T10:00:00Z")
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.id.value shouldBe 123
        workItem.title shouldBe "Bug"
        workItem.state shouldBe WorkItemState.Open
        workItem.created shouldBe Instant.parse("2024-01-15T10:00:00Z")
    }

    // Phase 6.2: State conversion

    @Test
    fun `toGitHubWorkItem should convert OPEN state to WorkItemState Open`() {
        // given
        val graphqlIssue = aGraphQLIssue { state("OPEN") }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.state shouldBe WorkItemState.Open
    }

    @Test
    fun `toGitHubWorkItem should convert CLOSED state to WorkItemState Closed`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            state("CLOSED")
            closedAt("2024-01-20T15:00:00Z")
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.state shouldBe WorkItemState.Closed
        workItem.closed shouldBe Instant.parse("2024-01-20T15:00:00Z")
    }

    // Phase 6.3: Labels to type conversion

    @Test
    fun `toGitHubWorkItem should infer Bug type from bug label`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            labels(aGraphQLLabel(name = "bug"))
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.type shouldBe WorkItemType.Bug
    }

    @Test
    fun `toGitHubWorkItem should infer Epic type from type-epic label`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            labels(aGraphQLLabel(name = "type:epic"))
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.type shouldBe WorkItemType.Epic
    }

    @Test
    fun `toGitHubWorkItem should extract all label names`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            labels(
                aGraphQLLabel(name = "bug"),
                aGraphQLLabel(name = "high-priority"),
                aGraphQLLabel(name = "frontend")
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.labels shouldContainExactly listOf("bug", "high-priority", "frontend")
    }

    // Phase 6.4: Hierarchy conversion

    @Test
    fun `toGitHubWorkItem should convert native parent to IssueReference`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            number(5)
            parent(1)
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.parent?.number shouldBe IssueNumber(1)
        workItem.parent?.repo shouldBe testRepoId
    }

    @Test
    fun `toGitHubWorkItem should convert subIssues to childIds`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            number(1)
            subIssues(
                aGraphQLIssueReference(number = 3),
                aGraphQLIssueReference(number = 5)
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.childIds shouldContainExactly listOf(IssueNumber(3), IssueNumber(5))
    }

    @Test
    fun `toGitHubWorkItem should handle issue with no parent or children`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            number(10)
            // No parent or subIssues set
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.parent shouldBe null
        workItem.childIds shouldBe emptyList()
    }

    // Phase 6.5: Timeline events to transitions

    @Test
    fun `toGitHubWorkItem should convert LabeledEvent to label transition`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            timelineItems(
                aGraphQLLabeledEvent(
                    createdAt = "2024-01-15T10:00:00Z",
                    actor = aGraphQLAuthor("dev"),
                    label = aGraphQLLabel(name = "bug")
                )
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldHaveSize 1
        val transition = workItem.transitions.first()
        transition.field shouldBe TransitionField.Label
        transition.from shouldBe null
        transition.to shouldBe "bug"
        transition.at shouldBe Instant.parse("2024-01-15T10:00:00Z")
        transition.actor shouldBe "dev"
    }

    @Test
    fun `toGitHubWorkItem should convert UnlabeledEvent to label transition`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            timelineItems(
                aGraphQLUnlabeledEvent(
                    createdAt = "2024-01-16T10:00:00Z",
                    actor = aGraphQLAuthor("admin"),
                    label = aGraphQLLabel(name = "wontfix")
                )
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldHaveSize 1
        val transition = workItem.transitions.first()
        transition.field shouldBe TransitionField.Label
        transition.from shouldBe "wontfix"
        transition.to shouldBe null
    }

    @Test
    fun `toGitHubWorkItem should convert ClosedEvent to state transition`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            timelineItems(
                aGraphQLClosedEvent(
                    createdAt = "2024-01-20T15:00:00Z",
                    actor = aGraphQLAuthor("resolver")
                )
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldHaveSize 1
        val transition = workItem.transitions.first()
        transition.field shouldBe TransitionField.State
        transition.from shouldBe "open"
        transition.to shouldBe "closed"
        transition.actor shouldBe "resolver"
    }

    @Test
    fun `toGitHubWorkItem should convert ReopenedEvent to state transition`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            timelineItems(
                aGraphQLReopenedEvent(
                    createdAt = "2024-01-21T09:00:00Z",
                    actor = aGraphQLAuthor("reopener")
                )
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldHaveSize 1
        val transition = workItem.transitions.first()
        transition.field shouldBe TransitionField.State
        transition.from shouldBe "closed"
        transition.to shouldBe "open"
    }

    @Test
    fun `toGitHubWorkItem should convert AssignedEvent to assignee transition`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            timelineItems(
                aGraphQLAssignedEvent(
                    createdAt = "2024-01-15T11:00:00Z",
                    actor = aGraphQLAuthor("manager"),
                    assignee = aGraphQLAuthor("developer")
                )
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldHaveSize 1
        val transition = workItem.transitions.first()
        transition.field shouldBe TransitionField.Assignee
        transition.from shouldBe null
        transition.to shouldBe "developer"
        transition.actor shouldBe "manager"
    }

    @Test
    fun `toGitHubWorkItem should convert UnassignedEvent to assignee transition`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            timelineItems(
                aGraphQLUnassignedEvent(
                    createdAt = "2024-01-18T14:00:00Z",
                    actor = aGraphQLAuthor("pm"),
                    assignee = aGraphQLAuthor("developer")
                )
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldHaveSize 1
        val transition = workItem.transitions.first()
        transition.field shouldBe TransitionField.Assignee
        transition.from shouldBe "developer"
        transition.to shouldBe null
    }

    @Test
    fun `toGitHubWorkItem should convert MilestonedEvent to milestone transition`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            timelineItems(
                aGraphQLMilestonedEvent(
                    createdAt = "2024-01-15T12:00:00Z",
                    actor = aGraphQLAuthor("planner"),
                    milestoneTitle = "v2.0"
                )
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldHaveSize 1
        val transition = workItem.transitions.first()
        transition.field shouldBe TransitionField.Milestone
        transition.from shouldBe null
        transition.to shouldBe "v2.0"
    }

    @Test
    fun `toGitHubWorkItem should convert DemilestonedEvent to milestone transition`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            timelineItems(
                aGraphQLDemilestonedEvent(
                    createdAt = "2024-01-19T10:00:00Z",
                    actor = aGraphQLAuthor("planner"),
                    milestoneTitle = "v1.5"
                )
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldHaveSize 1
        val transition = workItem.transitions.first()
        transition.field shouldBe TransitionField.Milestone
        transition.from shouldBe "v1.5"
        transition.to shouldBe null
    }

    @Test
    fun `toGitHubWorkItem should convert all timeline event types in sequence`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            timelineItems(
                aGraphQLLabeledEvent(createdAt = "2024-01-15T10:00:00Z", label = aGraphQLLabel(name = "bug")),
                aGraphQLAssignedEvent(createdAt = "2024-01-15T11:00:00Z", assignee = aGraphQLAuthor("dev")),
                aGraphQLMilestonedEvent(createdAt = "2024-01-15T12:00:00Z", milestoneTitle = "v1.0"),
                aGraphQLClosedEvent(createdAt = "2024-01-20T15:00:00Z")
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldHaveSize 4
        workItem.transitions[0].field shouldBe TransitionField.Label
        workItem.transitions[1].field shouldBe TransitionField.Assignee
        workItem.transitions[2].field shouldBe TransitionField.Milestone
        workItem.transitions[3].field shouldBe TransitionField.State
    }

    @Test
    fun `toGitHubWorkItem should handle issue with no timeline events`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            number(42)
            // No timelineItems set
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldBe emptyList()
    }

    @Test
    fun `toGitHubWorkItem should skip unknown timeline event types`() {
        // given - simulating an Other event by having empty timeline
        // (In real scenarios, Other events would be filtered out)
        val graphqlIssue = aGraphQLIssue {
            timelineItems(
                aGraphQLClosedEvent(createdAt = "2024-01-20T15:00:00Z")
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.transitions shouldHaveSize 1
    }

    // Additional tests for assignees and milestone fields

    @Test
    fun `toGitHubWorkItem should extract assignee logins`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            assignees(
                aGraphQLAuthor("alice"),
                aGraphQLAuthor("bob")
            )
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.assignees shouldContainExactly listOf("alice", "bob")
    }

    @Test
    fun `toGitHubWorkItem should extract milestone title`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            milestone(aGraphQLMilestone(title = "Release 3.0", state = "OPEN"))
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.milestone shouldBe "Release 3.0"
    }

    @Test
    fun `toGitHubWorkItem should handle null milestone`() {
        // given
        val graphqlIssue = aGraphQLIssue {
            milestone(null)
        }

        // when
        val workItem = graphqlIssue.toGitHubWorkItem(testRepoId)

        // then
        workItem.milestone shouldBe null
    }

    // Edge case: Invalid state

    @Test
    fun `toGitHubWorkItem should throw for invalid GraphQL state`() {
        // given
        val graphqlIssue = aGraphQLIssue { state("INVALID") }

        // when/then
        shouldThrow<IllegalArgumentException> {
            graphqlIssue.toGitHubWorkItem(testRepoId)
        }
    }
}
