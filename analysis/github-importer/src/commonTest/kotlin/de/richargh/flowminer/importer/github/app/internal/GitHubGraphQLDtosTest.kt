package de.richargh.flowminer.importer.github.app.internal

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class GitHubGraphQLDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GraphQLPageInfo should deserialize from JSON with cursor`() {
        // given
        val jsonString = """{ "hasNextPage": true, "endCursor": "abc123" }"""

        // when
        val pageInfo = json.decodeFromString<GraphQLPageInfo>(jsonString)

        // then
        pageInfo.hasNextPage shouldBe true
        pageInfo.endCursor shouldBe "abc123"
    }

    @Test
    fun `GraphQLPageInfo should deserialize from JSON with null cursor`() {
        // given
        val jsonString = """{ "hasNextPage": false, "endCursor": null }"""

        // when
        val pageInfo = json.decodeFromString<GraphQLPageInfo>(jsonString)

        // then
        pageInfo.hasNextPage shouldBe false
        pageInfo.endCursor shouldBe null
    }

    // Step 1.2: GraphQL Author DTO
    @Test
    fun `GraphQLAuthor should deserialize from JSON`() {
        // given
        val jsonString = """{ "login": "octocat" }"""

        // when
        val author = json.decodeFromString<GraphQLAuthor>(jsonString)

        // then
        author.login shouldBe "octocat"
    }

    // Step 1.3: GraphQL Label DTO
    @Test
    fun `GraphQLLabel should deserialize from JSON with color`() {
        // given
        val jsonString = """{ "name": "bug", "color": "d73a4a" }"""

        // when
        val label = json.decodeFromString<GraphQLLabel>(jsonString)

        // then
        label.name shouldBe "bug"
        label.color shouldBe "d73a4a"
    }

    @Test
    fun `GraphQLLabel should deserialize from JSON without color`() {
        // given
        val jsonString = """{ "name": "enhancement" }"""

        // when
        val label = json.decodeFromString<GraphQLLabel>(jsonString)

        // then
        label.name shouldBe "enhancement"
        label.color shouldBe null
    }

    // Step 1.4: GraphQL Milestone DTO
    @Test
    fun `GraphQLMilestone should deserialize OPEN state from JSON`() {
        // given
        val jsonString = """{ "title": "v1.0", "state": "OPEN" }"""

        // when
        val milestone = json.decodeFromString<GraphQLMilestone>(jsonString)

        // then
        milestone.title shouldBe "v1.0"
        milestone.state shouldBe "OPEN"
    }

    @Test
    fun `GraphQLMilestone should deserialize CLOSED state from JSON`() {
        // given
        val jsonString = """{ "title": "v2.0", "state": "CLOSED" }"""

        // when
        val milestone = json.decodeFromString<GraphQLMilestone>(jsonString)

        // then
        milestone.title shouldBe "v2.0"
        milestone.state shouldBe "CLOSED"
    }

    // Step 1.5: GraphQL Issue Reference DTO
    @Test
    fun `GraphQLIssueReference should deserialize from JSON`() {
        // given
        val jsonString = """{ "number": 42 }"""

        // when
        val issueRef = json.decodeFromString<GraphQLIssueReference>(jsonString)

        // then
        issueRef.number shouldBe 42
    }

    // Phase 2: Timeline Event DTOs (using sealed class variants)

    // Step 2.1: GraphQL LabeledEvent DTO
    @Test
    fun `GraphQLTimelineItem LabeledEvent should deserialize from JSON`() {
        // given
        val jsonString = """
            {
                "createdAt": "2024-01-01T10:00:00Z",
                "actor": { "login": "user" },
                "label": { "name": "bug" }
            }
        """.trimIndent()

        // when
        val event = json.decodeFromString<GraphQLTimelineItem.LabeledEvent>(jsonString)

        // then
        event.createdAt shouldBe "2024-01-01T10:00:00Z"
        event.actor?.login shouldBe "user"
        event.label.name shouldBe "bug"
    }

    // Step 2.2: GraphQL UnlabeledEvent DTO
    @Test
    fun `GraphQLTimelineItem UnlabeledEvent should deserialize from JSON`() {
        // given
        val jsonString = """
            {
                "createdAt": "2024-01-02T10:00:00Z",
                "actor": { "login": "user" },
                "label": { "name": "bug" }
            }
        """.trimIndent()

        // when
        val event = json.decodeFromString<GraphQLTimelineItem.UnlabeledEvent>(jsonString)

        // then
        event.createdAt shouldBe "2024-01-02T10:00:00Z"
        event.actor?.login shouldBe "user"
        event.label.name shouldBe "bug"
    }

    // Step 2.3: GraphQL AssignedEvent DTO
    @Test
    fun `GraphQLTimelineItem AssignedEvent should deserialize from JSON`() {
        // given
        val jsonString = """
            {
                "createdAt": "2024-01-03T10:00:00Z",
                "actor": { "login": "admin" },
                "assignee": { "login": "dev" }
            }
        """.trimIndent()

        // when
        val event = json.decodeFromString<GraphQLTimelineItem.AssignedEvent>(jsonString)

        // then
        event.createdAt shouldBe "2024-01-03T10:00:00Z"
        event.actor?.login shouldBe "admin"
        event.assignee?.login shouldBe "dev"
    }

    // Step 2.4: GraphQL UnassignedEvent DTO
    @Test
    fun `GraphQLTimelineItem UnassignedEvent should deserialize from JSON`() {
        // given
        val jsonString = """
            {
                "createdAt": "2024-01-04T10:00:00Z",
                "actor": { "login": "admin" },
                "assignee": { "login": "dev" }
            }
        """.trimIndent()

        // when
        val event = json.decodeFromString<GraphQLTimelineItem.UnassignedEvent>(jsonString)

        // then
        event.createdAt shouldBe "2024-01-04T10:00:00Z"
        event.actor?.login shouldBe "admin"
        event.assignee?.login shouldBe "dev"
    }

    // Step 2.5: GraphQL ClosedEvent DTO
    @Test
    fun `GraphQLTimelineItem ClosedEvent should deserialize from JSON`() {
        // given
        val jsonString = """
            {
                "createdAt": "2024-01-05T10:00:00Z",
                "actor": { "login": "user" }
            }
        """.trimIndent()

        // when
        val event = json.decodeFromString<GraphQLTimelineItem.ClosedEvent>(jsonString)

        // then
        event.createdAt shouldBe "2024-01-05T10:00:00Z"
        event.actor?.login shouldBe "user"
    }

    // Step 2.6: GraphQL ReopenedEvent DTO
    @Test
    fun `GraphQLTimelineItem ReopenedEvent should deserialize from JSON`() {
        // given
        val jsonString = """
            {
                "createdAt": "2024-01-06T10:00:00Z",
                "actor": { "login": "user" }
            }
        """.trimIndent()

        // when
        val event = json.decodeFromString<GraphQLTimelineItem.ReopenedEvent>(jsonString)

        // then
        event.createdAt shouldBe "2024-01-06T10:00:00Z"
        event.actor?.login shouldBe "user"
    }

    // Step 2.7: GraphQL MilestonedEvent DTO
    @Test
    fun `GraphQLTimelineItem MilestonedEvent should deserialize from JSON`() {
        // given
        val jsonString = """
            {
                "createdAt": "2024-01-07T10:00:00Z",
                "actor": { "login": "user" },
                "milestoneTitle": "v1.0"
            }
        """.trimIndent()

        // when
        val event = json.decodeFromString<GraphQLTimelineItem.MilestonedEvent>(jsonString)

        // then
        event.createdAt shouldBe "2024-01-07T10:00:00Z"
        event.actor?.login shouldBe "user"
        event.milestoneTitle shouldBe "v1.0"
    }

    // Step 2.8: GraphQL DemilestonedEvent DTO
    @Test
    fun `GraphQLTimelineItem DemilestonedEvent should deserialize from JSON`() {
        // given
        val jsonString = """
            {
                "createdAt": "2024-01-08T10:00:00Z",
                "actor": { "login": "user" },
                "milestoneTitle": "v1.0"
            }
        """.trimIndent()

        // when
        val event = json.decodeFromString<GraphQLTimelineItem.DemilestonedEvent>(jsonString)

        // then
        event.createdAt shouldBe "2024-01-08T10:00:00Z"
        event.actor?.login shouldBe "user"
        event.milestoneTitle shouldBe "v1.0"
    }

    // Step 2.9: Polymorphic TimelineItem container
    @Test
    fun `GraphQLTimelineItem should deserialize LabeledEvent with __typename`() {
        // given
        val jsonString = """
            {
                "__typename": "LabeledEvent",
                "createdAt": "2024-01-01T10:00:00Z",
                "actor": { "login": "user" },
                "label": { "name": "bug" }
            }
        """.trimIndent()

        // when
        val item = json.decodeFromString<GraphQLTimelineItem>(jsonString)

        // then
        item shouldBe GraphQLTimelineItem.LabeledEvent(
            createdAt = "2024-01-01T10:00:00Z",
            actor = GraphQLAuthor("user"),
            label = GraphQLLabel("bug")
        )
    }

    @Test
    fun `GraphQLTimelineItem should deserialize ClosedEvent with __typename`() {
        // given
        val jsonString = """
            {
                "__typename": "ClosedEvent",
                "createdAt": "2024-01-05T10:00:00Z",
                "actor": { "login": "user" }
            }
        """.trimIndent()

        // when
        val item = json.decodeFromString<GraphQLTimelineItem>(jsonString)

        // then
        item shouldBe GraphQLTimelineItem.ClosedEvent(
            createdAt = "2024-01-05T10:00:00Z",
            actor = GraphQLAuthor("user")
        )
    }

    @Test
    fun `GraphQLTimelineItem should deserialize unknown event type as Other`() {
        // given - An unknown timeline event type from GitHub
        val jsonString = """
            {
                "__typename": "CommentDeletedEvent",
                "createdAt": "2024-01-09T10:00:00Z"
            }
        """.trimIndent()

        // when
        val item = json.decodeFromString<GraphQLTimelineItem>(jsonString)

        // then
        item shouldBe GraphQLTimelineItem.Other("CommentDeletedEvent")
    }

    // Phase 3: GraphQL Issue DTO

    // Step 3.1: GraphQL Issue with basic fields
    @Test
    fun `GraphQLIssue should deserialize basic fields from JSON`() {
        // given
        val jsonString = """
            {
                "number": 1,
                "title": "Test Issue",
                "state": "OPEN",
                "body": "Issue description",
                "createdAt": "2024-01-01T10:00:00Z",
                "closedAt": null
            }
        """.trimIndent()

        // when
        val issue = json.decodeFromString<GraphQLIssue>(jsonString)

        // then
        issue.number shouldBe 1
        issue.title shouldBe "Test Issue"
        issue.state shouldBe "OPEN"
        issue.body shouldBe "Issue description"
        issue.createdAt shouldBe "2024-01-01T10:00:00Z"
        issue.closedAt shouldBe null
    }

    // Step 3.2: GraphQL Issue with author
    @Test
    fun `GraphQLIssue should deserialize with author from JSON`() {
        // given
        val jsonString = """
            {
                "number": 2,
                "title": "Issue with Author",
                "state": "OPEN",
                "body": null,
                "createdAt": "2024-01-01T10:00:00Z",
                "closedAt": null,
                "author": { "login": "octocat" }
            }
        """.trimIndent()

        // when
        val issue = json.decodeFromString<GraphQLIssue>(jsonString)

        // then
        issue.author?.login shouldBe "octocat"
    }

    // Step 3.3: GraphQL Issue with labels connection
    @Test
    fun `GraphQLIssue should deserialize with labels connection from JSON`() {
        // given
        val jsonString = """
            {
                "number": 3,
                "title": "Issue with Labels",
                "state": "OPEN",
                "body": null,
                "createdAt": "2024-01-01T10:00:00Z",
                "closedAt": null,
                "labels": { "nodes": [{ "name": "bug", "color": "d73a4a" }] }
            }
        """.trimIndent()

        // when
        val issue = json.decodeFromString<GraphQLIssue>(jsonString)

        // then
        issue.labels?.nodes?.size shouldBe 1
        issue.labels?.nodes?.get(0)?.name shouldBe "bug"
    }

    // Step 3.4: GraphQL Issue with assignees connection
    @Test
    fun `GraphQLIssue should deserialize with assignees connection from JSON`() {
        // given
        val jsonString = """
            {
                "number": 4,
                "title": "Issue with Assignees",
                "state": "OPEN",
                "body": null,
                "createdAt": "2024-01-01T10:00:00Z",
                "closedAt": null,
                "assignees": { "nodes": [{ "login": "dev" }] }
            }
        """.trimIndent()

        // when
        val issue = json.decodeFromString<GraphQLIssue>(jsonString)

        // then
        issue.assignees?.nodes?.size shouldBe 1
        issue.assignees?.nodes?.get(0)?.login shouldBe "dev"
    }

    // Step 3.5: GraphQL Issue with milestone
    @Test
    fun `GraphQLIssue should deserialize with milestone from JSON`() {
        // given
        val jsonString = """
            {
                "number": 5,
                "title": "Issue with Milestone",
                "state": "OPEN",
                "body": null,
                "createdAt": "2024-01-01T10:00:00Z",
                "closedAt": null,
                "milestone": { "title": "v1.0", "state": "OPEN" }
            }
        """.trimIndent()

        // when
        val issue = json.decodeFromString<GraphQLIssue>(jsonString)

        // then
        issue.milestone?.title shouldBe "v1.0"
        issue.milestone?.state shouldBe "OPEN"
    }

    // Step 3.6: GraphQL Issue with parent and subIssues
    @Test
    fun `GraphQLIssue should deserialize with parent and subIssues from JSON`() {
        // given
        val jsonString = """
            {
                "number": 6,
                "title": "Issue with Hierarchy",
                "state": "OPEN",
                "body": null,
                "createdAt": "2024-01-01T10:00:00Z",
                "closedAt": null,
                "parent": { "number": 1 },
                "subIssues": { "nodes": [{ "number": 7 }, { "number": 8 }] }
            }
        """.trimIndent()

        // when
        val issue = json.decodeFromString<GraphQLIssue>(jsonString)

        // then
        issue.parent?.number shouldBe 1
        issue.subIssues?.nodes?.size shouldBe 2
        issue.subIssues?.nodes?.get(0)?.number shouldBe 7
    }

    // Step 3.7: GraphQL Issue with timelineItems
    @Test
    fun `GraphQLIssue should deserialize with timelineItems from JSON`() {
        // given
        val jsonString = """
            {
                "number": 7,
                "title": "Issue with Timeline",
                "state": "CLOSED",
                "body": null,
                "createdAt": "2024-01-01T10:00:00Z",
                "closedAt": "2024-01-15T10:00:00Z",
                "timelineItems": {
                    "nodes": [
                        {
                            "__typename": "LabeledEvent",
                            "createdAt": "2024-01-02T10:00:00Z",
                            "actor": { "login": "user" },
                            "label": { "name": "bug" }
                        },
                        {
                            "__typename": "ClosedEvent",
                            "createdAt": "2024-01-15T10:00:00Z",
                            "actor": { "login": "user" }
                        }
                    ]
                }
            }
        """.trimIndent()

        // when
        val issue = json.decodeFromString<GraphQLIssue>(jsonString)

        // then
        issue.timelineItems?.nodes?.size shouldBe 2
        val labeledEvent = issue.timelineItems?.nodes?.get(0) as GraphQLTimelineItem.LabeledEvent
        labeledEvent.label.name shouldBe "bug"
        val closedEvent = issue.timelineItems.nodes[1] as GraphQLTimelineItem.ClosedEvent
        closedEvent.actor?.login shouldBe "user"
    }

    // Phase 4: GraphQL Response Wrapper DTOs

    // Step 4.1: GraphQL Response wrapper
    @Test
    fun `GraphQLResponse should deserialize full response from JSON`() {
        // given
        val jsonString = """
            {
                "data": {
                    "repository": {
                        "issues": {
                            "nodes": [
                                {
                                    "number": 1,
                                    "title": "First Issue",
                                    "state": "OPEN",
                                    "body": "Description",
                                    "createdAt": "2024-01-01T10:00:00Z",
                                    "closedAt": null
                                }
                            ],
                            "pageInfo": {
                                "hasNextPage": true,
                                "endCursor": "cursor123"
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        // when
        val response = json.decodeFromString<GraphQLResponse>(jsonString)

        // then
        response.data?.repository?.issues?.nodes?.size shouldBe 1
        response.data?.repository?.issues?.nodes?.get(0)?.number shouldBe 1
        response.data?.repository?.issues?.pageInfo?.hasNextPage shouldBe true
        response.data?.repository?.issues?.pageInfo?.endCursor shouldBe "cursor123"
    }

    // Step 4.2: GraphQL errors response
    @Test
    fun `GraphQLResponse should deserialize errors from JSON`() {
        // given
        val jsonString = """
            {
                "errors": [
                    {
                        "message": "Could not resolve to a Repository with the name 'owner/repo'.",
                        "type": "NOT_FOUND"
                    }
                ]
            }
        """.trimIndent()

        // when
        val response = json.decodeFromString<GraphQLResponse>(jsonString)

        // then
        response.errors?.size shouldBe 1
        response.errors?.get(0)?.message shouldBe "Could not resolve to a Repository with the name 'owner/repo'."
        response.errors?.get(0)?.type shouldBe "NOT_FOUND"
    }

    @Test
    fun `GraphQLResponse should deserialize rate limit error from JSON`() {
        // given
        val jsonString = """
            {
                "errors": [
                    {
                        "message": "API rate limit exceeded for user ID 12345",
                        "type": "RATE_LIMITED"
                    }
                ]
            }
        """.trimIndent()

        // when
        val response = json.decodeFromString<GraphQLResponse>(jsonString)

        // then
        response.errors?.get(0)?.type shouldBe "RATE_LIMITED"
    }
}
