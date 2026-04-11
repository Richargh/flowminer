package de.richargh.flowminer.importer.jirafixtures

import de.richargh.flowminer.importer.jira.app.api.IssueKey
import de.richargh.flowminer.importer.jira.app.api.StateTransition
import io.kotest.matchers.shouldBe
import kotlin.time.Instant
import kotlin.test.Test

class WorkItemBuilderTest {

    @Test
    fun `aWorkItem with defaults should create a WorkItem with default key`() {
        val workItem = aWorkItem()

        workItem.key shouldBe IssueKey("TEST-1")
    }

    @Test
    fun `aWorkItem with defaults should have empty transitions`() {
        val workItem = aWorkItem()

        workItem.transitions shouldBe emptyList()
    }

    @Test
    fun `aWorkItem with defaults should have null started and finished`() {
        val workItem = aWorkItem()

        workItem.started shouldBe null
        workItem.finished shouldBe null
    }

    @Test
    fun `aWorkItem with all fields set should preserve them`() {
        val transition = StateTransition(from = "To Do", to = "In Progress", at = Instant.parse("2024-06-01T10:00:00Z"))
        val workItem = aWorkItem {
            key = IssueKey("PROJ-42")
            name = "Custom Issue"
            type = "Bug"
            state = "In Progress"
            started = Instant.parse("2024-06-01T10:00:00Z")
            finished = Instant.parse("2024-06-02T12:00:00Z")
            transitions = listOf(transition)
        }

        workItem.key shouldBe IssueKey("PROJ-42")
        workItem.name shouldBe "Custom Issue"
        workItem.type shouldBe "Bug"
        workItem.state shouldBe "In Progress"
        workItem.started shouldBe Instant.parse("2024-06-01T10:00:00Z")
        workItem.finished shouldBe Instant.parse("2024-06-02T12:00:00Z")
        workItem.transitions shouldBe listOf(transition)
    }
}
