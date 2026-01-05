package de.richargh.teamcharta.importer.github.app.internal

import de.richargh.teamcharta.importer.github.app.api.RepositoryId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class GitHubGraphQLClientTest {

    // Phase 7.1: Query string builder

    @Test
    fun `buildIssuesQuery should generate GraphQL query for repository issues`() {
        // given
        val repoId = RepositoryId("owner", "repo")
        val cursor: String? = null

        // when
        val request = buildIssuesQuery(repoId, cursor)

        // then
        request.query shouldContain "repository(owner:"
        request.query shouldContain "issues(first:"
        request.query shouldContain "pageInfo"
        request.query shouldContain "hasNextPage"
        request.query shouldContain "endCursor"
        request.query shouldContain "nodes"
        request.variables["owner"] shouldBe "owner"
        request.variables["name"] shouldBe "repo"
    }

    // Phase 7.2: Query with cursor

    @Test
    fun `buildIssuesQuery should include cursor in query when provided`() {
        // given
        val repoId = RepositoryId("owner", "repo")
        val cursor = "abc123"

        // when
        val request = buildIssuesQuery(repoId, cursor)

        // then
        request.query shouldContain "after:"
        request.variables["cursor"] shouldBe "abc123"
    }

    @Test
    fun `buildIssuesQuery should not include cursor when null`() {
        // given
        val repoId = RepositoryId("owner", "repo")
        val cursor: String? = null

        // when
        val request = buildIssuesQuery(repoId, cursor)

        // then
        request.variables.containsKey("cursor") shouldBe false
    }

    // Phase 7.3: Query contains all required fields

    @Test
    fun `buildIssuesQuery should include issue fields`() {
        // given
        val repoId = RepositoryId("owner", "repo")

        // when
        val request = buildIssuesQuery(repoId, null)

        // then - basic issue fields
        request.query shouldContain "number"
        request.query shouldContain "title"
        request.query shouldContain "state"
        request.query shouldContain "body"
        request.query shouldContain "createdAt"
        request.query shouldContain "closedAt"
    }

    @Test
    fun `buildIssuesQuery should include author and assignees`() {
        // given
        val repoId = RepositoryId("owner", "repo")

        // when
        val request = buildIssuesQuery(repoId, null)

        // then
        request.query shouldContain "author"
        request.query shouldContain "assignees"
        request.query shouldContain "login"
    }

    @Test
    fun `buildIssuesQuery should include labels and milestone`() {
        // given
        val repoId = RepositoryId("owner", "repo")

        // when
        val request = buildIssuesQuery(repoId, null)

        // then
        request.query shouldContain "labels"
        request.query shouldContain "milestone"
    }

    @Test
    fun `buildIssuesQuery should include parent and subIssues for hierarchy`() {
        // given
        val repoId = RepositoryId("owner", "repo")

        // when
        val request = buildIssuesQuery(repoId, null)

        // then
        request.query shouldContain "parent"
        request.query shouldContain "subIssues"
    }

    @Test
    fun `buildIssuesQuery should include timelineItems for history`() {
        // given
        val repoId = RepositoryId("owner", "repo")

        // when
        val request = buildIssuesQuery(repoId, null)

        // then
        request.query shouldContain "timelineItems"
        request.query shouldContain "__typename"
        request.query shouldContain "LabeledEvent"
        request.query shouldContain "ClosedEvent"
    }
}
