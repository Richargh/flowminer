package de.richargh.flowminer.importer.githubfixtures

import de.richargh.flowminer.importer.github.app.internal.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class GitHubGraphQLDtoBuildersTest {

    @Test
    fun `aGraphQLIssue builder should create issue with defaults`() {
        // when
        val issue = aGraphQLIssue { }

        // then
        issue.number shouldBe 1
        issue.title shouldBe "Test issue"
        issue.state shouldBe "OPEN"
        issue.createdAt shouldBe "2024-01-01T10:00:00Z"
    }

    @Test
    fun `aGraphQLIssue builder should create issue with specified values`() {
        // when
        val issue = aGraphQLIssue {
            number(42)
            title("Custom Issue")
            state("CLOSED")
            author("user123")
            labels(aGraphQLLabel("bug"), aGraphQLLabel("priority-high"))
            milestone("v2.0")
        }

        // then
        issue.number shouldBe 42
        issue.title shouldBe "Custom Issue"
        issue.state shouldBe "CLOSED"
        issue.author?.login shouldBe "user123"
        issue.labels?.nodes?.size shouldBe 2
        issue.milestone?.title shouldBe "v2.0"
    }

    @Test
    fun `aGraphQLIssue builder should support parent and subIssues`() {
        // when
        val issue = aGraphQLIssue {
            number(5)
            parent(1)
            subIssues(aGraphQLIssueReference(6), aGraphQLIssueReference(7))
        }

        // then
        issue.parent?.number shouldBe 1
        issue.subIssues?.nodes?.size shouldBe 2
        issue.subIssues?.nodes?.get(0)?.number shouldBe 6
    }

    @Test
    fun `aGraphQLIssue builder should support timeline items`() {
        // when
        val issue = aGraphQLIssue {
            number(10)
            timelineItems(
                aGraphQLLabeledEvent(label = aGraphQLLabel("bug")),
                aGraphQLClosedEvent()
            )
        }

        // then
        issue.timelineItems?.nodes?.size shouldBe 2
        val labeledEvent = issue.timelineItems?.nodes?.get(0) as GraphQLTimelineItem.LabeledEvent
        labeledEvent.label.name shouldBe "bug"
    }

    @Test
    fun `aGraphQLLabeledEvent should create event with defaults`() {
        // when
        val event = aGraphQLLabeledEvent()

        // then
        event.createdAt shouldBe "2024-01-15T10:00:00Z"
        event.actor?.login shouldBe "octocat"
        event.label.name shouldBe "bug"
    }

    @Test
    fun `aGraphQLClosedEvent should create event with defaults`() {
        // when
        val event = aGraphQLClosedEvent()

        // then
        event.createdAt shouldBe "2024-01-15T10:00:00Z"
        event.actor?.login shouldBe "octocat"
    }

    @Test
    fun `aGraphQLResponse builder should create response with issues`() {
        // when
        val response = aGraphQLResponse {
            issues(
                aGraphQLIssue { number(1); title("First") },
                aGraphQLIssue { number(2); title("Second") }
            )
        }

        // then
        response.data shouldNotBe null
        response.data?.repository?.issues?.nodes?.size shouldBe 2
        response.data?.repository?.issues?.nodes?.get(0)?.title shouldBe "First"
    }

    @Test
    fun `aGraphQLResponse builder should create response with pagination`() {
        // when
        val response = aGraphQLResponse {
            issues(aGraphQLIssue { })
            hasNextPage(true, "cursor123")
        }

        // then
        response.data?.repository?.issues?.pageInfo?.hasNextPage shouldBe true
        response.data?.repository?.issues?.pageInfo?.endCursor shouldBe "cursor123"
    }

    @Test
    fun `aGraphQLResponse builder should create error response`() {
        // when
        val response = aGraphQLResponse {
            errors(
                aGraphQLError("Not found", "NOT_FOUND")
            )
        }

        // then
        response.data shouldBe null
        response.errors?.size shouldBe 1
        response.errors?.get(0)?.message shouldBe "Not found"
        response.errors?.get(0)?.type shouldBe "NOT_FOUND"
    }
}
