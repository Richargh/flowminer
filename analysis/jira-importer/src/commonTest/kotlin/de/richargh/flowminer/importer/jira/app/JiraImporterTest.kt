package de.richargh.flowminer.importer.jira.app

import de.richargh.flowminer.importer.jira.app.api.IssueKey
import de.richargh.flowminer.importer.jira.app.api.JiraAuth
import de.richargh.flowminer.importer.jira.app.api.JiraCredentials
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.time.Instant

private val testJson = Json { ignoreUnknownKeys = true }

private fun createMockHttpClient(mockEngine: MockEngine): HttpClient {
    return HttpClient(mockEngine) {
        install(ContentNegotiation) { json(testJson) }
    }
}

private val testCredentials = JiraCredentials(
    auth = JiraAuth.Basic("user", "test-token"),
    baseUrl = "https://example.atlassian.net",
    apiVersion = "3"
)

class JiraImporterTest {

    @Test
    fun `should handle multiple issues with varied state transitions`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""
                    {"issues":[
                      {"key":"PROJ-1","fields":{"summary":"With transition"},"changelog":{"histories":[
                        {"created":"2024-06-01T10:00:00.000+0000","items":[{"field":"status","fromString":"To Do","toString":"In Progress"}]}
                      ]}},
                      {"key":"PROJ-2","fields":{"summary":"No changelog"},"changelog":{"histories":[]}},
                      {"key":"PROJ-3","fields":{"summary":"Done"},"changelog":{"histories":[
                        {"created":"2024-06-03T08:00:00.000+0000","items":[{"field":"status","fromString":"In Progress","toString":"Done"}]}
                      ]}}
                    ]}
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val importer = JiraImporter(testCredentials, createMockHttpClient(mockEngine))

        val workItems = importer.import("PROJ")

        workItems.size shouldBe 3
        workItems[0].key shouldBe IssueKey("PROJ-1")
        workItems[0].started shouldBe Instant.parse("2024-06-01T10:00:00Z")
        workItems[0].finished shouldBe null

        workItems[1].key shouldBe IssueKey("PROJ-2")
        workItems[1].transitions shouldBe emptyList()
        workItems[1].started shouldBe null

        workItems[2].key shouldBe IssueKey("PROJ-3")
        workItems[2].finished shouldBe Instant.parse("2024-06-03T08:00:00Z")
    }

    @Test
    fun `should import issues and return WorkItems`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""
                    {"issues":[
                      {"key":"PROJ-1","fields":{"summary":"Test Issue","issuetype":{"name":"Bug"},"status":{"name":"In Progress"}},
                       "changelog":{"histories":[
                         {"created":"2024-06-01T10:00:00.000+0000","items":[
                           {"field":"status","fromString":"To Do","toString":"In Progress"}
                         ]}
                       ]}}
                    ]}
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val importer = JiraImporter(testCredentials, createMockHttpClient(mockEngine))

        val workItems = importer.import("PROJ")

        workItems.size shouldBe 1
        val item = workItems[0]
        item.key shouldBe IssueKey("PROJ-1")
        item.name shouldBe "Test Issue"
        item.type shouldBe "Bug"
        item.state shouldBe "In Progress"
        item.started shouldBe Instant.parse("2024-06-01T10:00:00Z")
    }

    @Test
    fun `should paginate and report progress when total exceeds page size`() = runTest {
        val page1 = """{"total":2,"startAt":0,"issues":[{"key":"PROJ-1","fields":{"summary":"First","issuetype":{"name":"Bug"},"status":{"name":"Open"}},"changelog":{"histories":[]}}]}"""
        val page2 = """{"total":2,"startAt":1,"issues":[{"key":"PROJ-2","fields":{"summary":"Second","issuetype":{"name":"Task"},"status":{"name":"Done"}},"changelog":{"histories":[]}}]}"""
        var callCount = 0
        val mockEngine = MockEngine { request ->
            callCount++
            val startAt = request.url.parameters["startAt"]?.toInt() ?: 0
            respond(
                content = ByteReadChannel(if (startAt == 0) page1 else page2),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val importer = JiraImporter(testCredentials, createMockHttpClient(mockEngine))
        val workItems = importer.import("PROJ", pageSize = 1) { fetched, total ->
            progressUpdates.add(fetched to total)
        }

        workItems.size shouldBe 2
        callCount shouldBe 2
        progressUpdates shouldBe listOf(1 to 2, 2 to 2)
    }
}
