package de.richargh.flowminer.importer.jirafixtures

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JiraApiDtoBuilderTest {

    @Test
    fun `aJiraSearchResponse with defaults should create a response with no issues`() {
        val response = aJiraSearchResponse()

        response.issues shouldBe emptyList()
    }

    @Test
    fun `aJiraSearchResponse with one issue should contain it`() {
        val response = aJiraSearchResponse {
            issues(aJiraIssue { key = "PROJ-1" })
        }

        response.issues.size shouldBe 1
        response.issues[0].key shouldBe "PROJ-1"
    }

    @Test
    fun `aJiraIssue with defaults should have key TEST-1`() {
        val issue = aJiraIssue()

        issue.key shouldBe "TEST-1"
        issue.fields shouldBe null
        issue.changelog shouldBe null
    }

    @Test
    fun `aJiraFields with defaults should have summary Test Issue`() {
        val fields = aJiraFields()

        fields.summary shouldBe "Test Issue"
        fields.issuetype shouldBe null
        fields.status shouldBe null
    }

    @Test
    fun `aJiraHistory with defaults should have a created timestamp`() {
        val history = aJiraHistory()

        history.created shouldBe "2024-06-01T10:00:00.000+0000"
        history.items shouldBe emptyList()
    }

    @Test
    fun `aJiraHistoryItem with defaults should have field status`() {
        val item = aJiraHistoryItem()

        item.field shouldBe "status"
        item.fromString shouldBe null
        item.toStringValue shouldBe null
    }

    @Test
    fun `aJiraHistoryItem should set fromStatus and toStatus`() {
        val item = aJiraHistoryItem {
            fromStatus = "To Do"
            toStatus = "In Progress"
        }

        item.fromString shouldBe "To Do"
        item.toStringValue shouldBe "In Progress"
    }
}
