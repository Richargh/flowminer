package de.richargh.teamcharta.importer.github.app

import de.richargh.teamcharta.importer.github.app.api.GitHubCredentials
import de.richargh.teamcharta.importer.github.app.api.RepositoryId
import de.richargh.teamcharta.importer.github.app.api.WorkItemState
import de.richargh.teamcharta.importer.githubfixtures.aGraphQLAuthor
import de.richargh.teamcharta.importer.githubfixtures.aGraphQLClosedEvent
import de.richargh.teamcharta.importer.githubfixtures.aGraphQLIssue
import de.richargh.teamcharta.importer.githubfixtures.aGraphQLIssueReference
import de.richargh.teamcharta.importer.githubfixtures.aGraphQLLabel
import de.richargh.teamcharta.importer.githubfixtures.aGraphQLLabeledEvent
import de.richargh.teamcharta.importer.githubfixtures.aGraphQLResponse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.time.Instant

class GitHubImporterTest {

    private val credentials = GitHubCredentials(token = "test-token")
    private val repoId = RepositoryId("octocat", "hello-world")
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `fetchIssuesWithTimelines should return list of GitHubWorkItem`() = runTest {
        // given
        val graphqlResponse = aGraphQLResponse {
            issues(
                aGraphQLIssue {
                    number(1)
                    title("Bug report")
                    state("OPEN")
                    createdAt("2024-01-01T10:00:00Z")
                },
                aGraphQLIssue {
                    number(2)
                    title("Feature request")
                    state("CLOSED")
                    createdAt("2024-01-02T10:00:00Z")
                    closedAt("2024-01-15T10:00:00Z")
                }
            )
        }
        val responseJson = json.encodeToString(graphqlResponse)

        val mockEngine = MockEngine.Companion { _ ->
            respond(
                content = responseJson,
                status = HttpStatusCode.Companion.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }

        val service = GitHubImporter(credentials, httpClient)

        // when
        val workItems = service.fetchIssuesWithTimelines(repoId)

        // then
        workItems shouldHaveSize 2
        workItems[0].id.value shouldBe 1
        workItems[0].title shouldBe "Bug report"
        workItems[0].state shouldBe WorkItemState.Open
        workItems[1].id.value shouldBe 2
        workItems[1].title shouldBe "Feature request"
        workItems[1].state shouldBe WorkItemState.Closed
    }

    @Test
    fun `fetchIssuesWithTimelines should return empty list for repository with no issues`() = runTest {
        // given
        val graphqlResponse = aGraphQLResponse {
            // no issues
        }
        val responseJson = json.encodeToString(graphqlResponse)

        val mockEngine = MockEngine.Companion { _ ->
            respond(
                content = responseJson,
                status = HttpStatusCode.Companion.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }

        val service = GitHubImporter(credentials, httpClient)

        // when
        val workItems = service.fetchIssuesWithTimelines(repoId)

        // then
        workItems shouldBe emptyList()
    }

    @Test
    fun `fetchIssuesWithTimelines should populate transitions for each issue`() = runTest {
        // given
        val graphqlResponse = aGraphQLResponse {
            issues(
                aGraphQLIssue {
                    number(1)
                    title("Bug report")
                    state("CLOSED")
                    createdAt("2024-01-01T10:00:00Z")
                    closedAt("2024-01-15T10:00:00Z")
                    timelineItems(
                        aGraphQLLabeledEvent(
                            createdAt = "2024-01-02T10:00:00Z",
                            actor = aGraphQLAuthor("octocat"),
                            label = aGraphQLLabel(name = "bug")
                        ),
                        aGraphQLClosedEvent(
                            createdAt = "2024-01-15T10:00:00Z",
                            actor = aGraphQLAuthor("octocat")
                        )
                    )
                }
            )
        }
        val responseJson = json.encodeToString(graphqlResponse)

        val mockEngine = MockEngine.Companion { _ ->
            respond(
                content = responseJson,
                status = HttpStatusCode.Companion.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }

        val service = GitHubImporter(credentials, httpClient)

        // when
        val workItems = service.fetchIssuesWithTimelines(repoId)

        // then
        workItems shouldHaveSize 1
        workItems[0].id.value shouldBe 1
        workItems[0].transitions shouldHaveSize 2
        workItems[0].transitions[0].field shouldBe "label"
        workItems[0].transitions[0].to shouldBe "bug"
        workItems[0].transitions[1].field shouldBe "state"
        workItems[0].transitions[1].to shouldBe "closed"
    }

    @Test
    fun `fetchIssuesWithTimelines should handle issues with no timeline events`() = runTest {
        // given
        val graphqlResponse = aGraphQLResponse {
            issues(
                aGraphQLIssue {
                    number(1)
                    title("New issue")
                    state("OPEN")
                    createdAt("2024-01-01T10:00:00Z")
                    // No timelineItems
                }
            )
        }
        val responseJson = json.encodeToString(graphqlResponse)

        val mockEngine = MockEngine.Companion { _ ->
            respond(
                content = responseJson,
                status = HttpStatusCode.Companion.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }

        val service = GitHubImporter(credentials, httpClient)

        // when
        val workItems = service.fetchIssuesWithTimelines(repoId)

        // then
        workItems shouldHaveSize 1
        workItems[0].transitions shouldBe emptyList()
    }

    @Test
    fun `fetchIssuesWithTimelines should detect parent-child via native hierarchy`() = runTest {
        // given
        val graphqlResponse = aGraphQLResponse {
            issues(
                aGraphQLIssue {
                    number(123)
                    title("Epic")
                    state("OPEN")
                    createdAt("2024-01-01T10:00:00Z")
                    subIssues(aGraphQLIssueReference(number = 124))
                },
                aGraphQLIssue {
                    number(124)
                    title("Story")
                    state("OPEN")
                    createdAt("2024-01-02T10:00:00Z")
                    parent(123)
                }
            )
        }
        val responseJson = json.encodeToString(graphqlResponse)

        val mockEngine = MockEngine.Companion { _ ->
            respond(
                content = responseJson,
                status = HttpStatusCode.Companion.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }

        val service = GitHubImporter(credentials, httpClient)

        // when
        val workItems = service.fetchIssuesWithTimelines(repoId)

        // then
        workItems shouldHaveSize 2

        // Issue #123 (Epic) should have #124 as child
        val epic = workItems.first { it.id.value == 123 }
        epic.childIds.map { it.value } shouldBe listOf(124)

        // Issue #124 (Story) should have #123 as parent
        val story = workItems.first { it.id.value == 124 }
        story.parent?.number?.value shouldBe 123
    }

    @Test
    fun `fetchIssuesWithTimelines should preserve cross-repo parent references`() = runTest {
        // given - issue references parent #999 in a different repository
        val graphqlResponse = aGraphQLResponse {
            issues(
                aGraphQLIssue {
                    number(124)
                    title("Story with cross-repo parent")
                    state("OPEN")
                    createdAt("2024-01-02T10:00:00Z")
                    parent(999, "other-owner", "other-repo")
                }
            )
        }
        val responseJson = json.encodeToString(graphqlResponse)

        val mockEngine = MockEngine.Companion { _ ->
            respond(
                content = responseJson,
                status = HttpStatusCode.Companion.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }

        val service = GitHubImporter(credentials, httpClient)

        // when
        val workItems = service.fetchIssuesWithTimelines(repoId)

        // then - cross-repo parent reference should be preserved
        workItems shouldHaveSize 1
        workItems[0].parent?.number?.value shouldBe 999
        workItems[0].parent?.repo?.owner shouldBe "other-owner"
        workItems[0].parent?.repo?.name shouldBe "other-repo"
    }

    @Test
    fun `fetchIssuesWithTimelines should support deeply nested hierarchies`() = runTest {
        // given - Epic -> Feature -> Story -> Task (4 levels)
        val graphqlResponse = aGraphQLResponse {
            issues(
                aGraphQLIssue {
                    number(1)
                    title("Epic")
                    state("OPEN")
                    createdAt("2024-01-01T10:00:00Z")
                    subIssues(aGraphQLIssueReference(number = 2))
                },
                aGraphQLIssue {
                    number(2)
                    title("Feature")
                    state("OPEN")
                    createdAt("2024-01-02T10:00:00Z")
                    parent(1)
                    subIssues(aGraphQLIssueReference(number = 3))
                },
                aGraphQLIssue {
                    number(3)
                    title("Story")
                    state("OPEN")
                    createdAt("2024-01-03T10:00:00Z")
                    parent(2)
                    subIssues(aGraphQLIssueReference(number = 4))
                },
                aGraphQLIssue {
                    number(4)
                    title("Task")
                    state("OPEN")
                    createdAt("2024-01-04T10:00:00Z")
                    parent(3)
                }
            )
        }
        val responseJson = json.encodeToString(graphqlResponse)

        val mockEngine = MockEngine.Companion { _ ->
            respond(
                content = responseJson,
                status = HttpStatusCode.Companion.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }

        val service = GitHubImporter(credentials, httpClient)

        // when
        val workItems = service.fetchIssuesWithTimelines(repoId)

        // then
        workItems shouldHaveSize 4

        // Verify the chain: Epic(1) <- Feature(2) <- Story(3) <- Task(4)
        val epic = workItems.first { it.id.value == 1 }
        epic.parent shouldBe null
        epic.childIds.map { it.value } shouldBe listOf(2)

        val feature = workItems.first { it.id.value == 2 }
        feature.parent?.number?.value shouldBe 1
        feature.childIds.map { it.value } shouldBe listOf(3)

        val story = workItems.first { it.id.value == 3 }
        story.parent?.number?.value shouldBe 2
        story.childIds.map { it.value } shouldBe listOf(4)

        val task = workItems.first { it.id.value == 4 }
        task.parent?.number?.value shouldBe 3
        task.childIds shouldBe emptyList()
    }

    @Test
    fun `fetchIssuesWithTimelines should use closed date from issue when available`() = runTest {
        // given
        val graphqlResponse = aGraphQLResponse {
            issues(
                aGraphQLIssue {
                    number(1)
                    title("Closed issue")
                    state("CLOSED")
                    createdAt("2024-01-01T10:00:00Z")
                    closedAt("2024-01-15T14:30:00Z")
                }
            )
        }
        val responseJson = json.encodeToString(graphqlResponse)

        val mockEngine = MockEngine.Companion { _ ->
            respond(
                content = responseJson,
                status = HttpStatusCode.Companion.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }

        val service = GitHubImporter(credentials, httpClient)

        // when
        val workItems = service.fetchIssuesWithTimelines(repoId)

        // then
        workItems shouldHaveSize 1
        workItems[0].closed shouldBe Instant.Companion.parse("2024-01-15T14:30:00Z")
    }
}