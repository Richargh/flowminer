package de.richargh.teamcharta.importer.github

import de.richargh.teamcharta.importer.github.app.api.TransitionField
import de.richargh.teamcharta.importer.github.app.api.WorkItemState
import de.richargh.teamcharta.importer.github.app.api.WorkItemType
import de.richargh.teamcharta.importer.github.app.toDto
import de.richargh.teamcharta.importer.githubfixtures.aGitHubWorkItem
import de.richargh.teamcharta.importer.githubfixtures.aStateTransition
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.time.Instant

class GithubDtoConversionTest {

    @Test
    fun `StateTransition toDto should convert to SerializableStateTransitionDto`() {
        // given
        val instant = Instant.parse("2024-01-15T10:00:00Z")
        val transition = aStateTransition {
            field(TransitionField.State)
            from("open")
            to("closed")
            at(instant)
            actor("octocat")
        }

        // when
        val dto = transition.toDto()

        // then
        dto.field shouldBe "State"
        dto.from shouldBe "open"
        dto.to shouldBe "closed"
        dto.at shouldBe "2024-01-15T10:00:00Z"
        dto.actor shouldBe "octocat"
    }

    @Test
    fun `SerializableStateTransitionDto should serialize to JSON`() {
        // given
        val instant = Instant.parse("2024-01-15T10:00:00Z")
        val transition = aStateTransition {
            field(TransitionField.State)
            from("open")
            to("closed")
            at(instant)
            actor("octocat")
        }
        val dto = transition.toDto()

        // when
        val json = Json.encodeToString(dto)

        // then
        json shouldBe """{"field":"State","from":"open","to":"closed","at":"2024-01-15T10:00:00Z","actor":"octocat"}"""
    }

    @Test
    fun `GitHubWorkItem toDto should convert to SerializableGitHubWorkItemDto`() {
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

        // when
        val dto = workItem.toDto()

        // then
        dto.id shouldBe 123
        dto.title shouldBe "Fix bug"
        dto.state shouldBe "Closed"
        dto.type shouldBe "Bug"
        dto.labels shouldBe listOf("bug", "priority:high")
        dto.assignees shouldBe listOf("octocat", "hubot")
        dto.milestone shouldBe "v1.0"
        dto.parentId shouldBe 100
        dto.childIds shouldBe listOf(124, 125)
        dto.created shouldBe "2024-01-01T10:00:00Z"
        dto.closed shouldBe "2024-01-15T10:00:00Z"
        dto.transitions.size shouldBe 1
    }

    @Test
    fun `SerializableGitHubWorkItemDto should serialize to JSON`() {
        // given
        val workItem = aGitHubWorkItem {
            id(1)
            title("Test")
            state(WorkItemState.Open)
        }
        val dto = workItem.toDto()

        // when
        val json = Json.encodeToString(dto)

        // then
        json.contains("\"id\":1") shouldBe true
        json.contains("\"title\":\"Test\"") shouldBe true
        json.contains("\"state\":\"Open\"") shouldBe true
    }
}
