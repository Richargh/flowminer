package de.richargh.flowminer.importer.jira.app.internal

import de.richargh.flowminer.importer.jira.app.api.JiraAuth
import de.richargh.flowminer.importer.jira.app.api.JiraConnectionException
import de.richargh.flowminer.importer.jira.app.api.JiraCredentials
import de.richargh.flowminer.importer.jira.app.api.JiraVersionDetectionException
import de.richargh.flowminer.importer.jira.app.api.JiraForbiddenException
import de.richargh.flowminer.importer.jira.app.api.JiraNotFoundException
import de.richargh.flowminer.importer.jira.app.api.JiraRateLimitException
import de.richargh.flowminer.importer.jira.app.api.JiraUnauthorizedException
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
import kotlin.test.assertFailsWith

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

private val patCredentials = JiraCredentials(
    auth = JiraAuth.Pat("my-pat-token"),
    baseUrl = "https://example.atlassian.net",
    apiVersion = "3"
)

private val v2Credentials = JiraCredentials(
    auth = JiraAuth.Pat("my-pat-token"),
    baseUrl = "https://example.atlassian.net",
    apiVersion = "2"
)

private val autoCredentials = JiraCredentials(
    auth = JiraAuth.Pat("my-pat-token"),
    baseUrl = "https://example.atlassian.net",
    apiVersion = "auto"
)

private fun clientFor(mockEngine: MockEngine) =
    JiraApiClient(testCredentials, createMockHttpClient(mockEngine))

class JiraApiClientTest {

    @Test
    fun `should fetch a page of issues from Jira API`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{"total":1,"startAt":0,"issues":[{"key":"PROJ-1","fields":{"summary":"Test Issue","issuetype":{"name":"Bug"},"status":{"name":"In Progress"}},"changelog":{"histories":[]}}]}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val response = clientFor(mockEngine).fetchIssuePage("PROJ", startAt = 0, pageSize = 100)

        response.total shouldBe 1
        response.issues.size shouldBe 1
        val issue = response.issues[0]
        issue.key shouldBe "PROJ-1"
        issue.fields?.summary shouldBe "Test Issue"
        issue.fields?.issuetype?.name shouldBe "Bug"
        issue.fields?.status?.name shouldBe "In Progress"
    }

    @Test
    fun `should pass startAt and pageSize in request url`() = runTest {
        val mockEngine = MockEngine { request ->
            request.url.parameters["startAt"] shouldBe "50"
            request.url.parameters["maxResults"] shouldBe "25"
            respond(
                content = ByteReadChannel("""{"total":75,"startAt":50,"issues":[]}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        clientFor(mockEngine).fetchIssuePage("PROJ", startAt = 50, pageSize = 25)
    }

    @Test
    fun `should throw JiraConnectionException when server is unreachable`() = runTest {
        val mockEngine = MockEngine { _ -> throw Exception("Connection refused") }

        assertFailsWith<JiraConnectionException> { clientFor(mockEngine).fetchIssuePage("PROJ", 0, 100) }
    }

    @Test
    fun `should throw JiraUnauthorizedException on 401`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{"message":"401 Unauthorized"}"""),
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        assertFailsWith<JiraUnauthorizedException> { clientFor(mockEngine).fetchIssuePage("PROJ", 0, 100) }
    }

    @Test
    fun `should throw JiraForbiddenException on 403`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.Forbidden,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        assertFailsWith<JiraForbiddenException> { clientFor(mockEngine).fetchIssuePage("PROJ", 0, 100) }
    }

    @Test
    fun `should throw JiraNotFoundException on 404`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        assertFailsWith<JiraNotFoundException> { clientFor(mockEngine).fetchIssuePage("PROJ", 0, 100) }
    }

    @Test
    fun `should throw JiraRateLimitException on 429`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        assertFailsWith<JiraRateLimitException> { clientFor(mockEngine).fetchIssuePage("PROJ", 0, 100) }
    }

    @Test
    fun `should auto-detect v3 when serverInfo returns 200 for v3`() = runTest {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath == "/rest/api/3/serverInfo" ->
                    respond("""{"serverTitle":"Jira"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath == "/rest/api/3/search/jql" ->
                    respond("""{"issues":[]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                else -> error("Unexpected: ${request.url.encodedPath}")
            }
        }
        JiraApiClient(autoCredentials, createMockHttpClient(mockEngine)).fetchIssuePage("PROJ", 0, 100)
    }

    @Test
    fun `should auto-detect v2 when v3 serverInfo fails`() = runTest {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath == "/rest/api/3/serverInfo" ->
                    respond("", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath == "/rest/api/2/serverInfo" ->
                    respond("""{"serverTitle":"Jira"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath == "/rest/api/2/search" ->
                    respond("""{"issues":[]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                else -> error("Unexpected: ${request.url.encodedPath}")
            }
        }
        JiraApiClient(autoCredentials, createMockHttpClient(mockEngine)).fetchIssuePage("PROJ", 0, 100)
    }

    @Test
    fun `should throw JiraVersionDetectionException when neither v3 nor v2 responds`() = runTest {
        val mockEngine = MockEngine { request ->
            respond("", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        assertFailsWith<JiraVersionDetectionException> {
            JiraApiClient(autoCredentials, createMockHttpClient(mockEngine)).fetchIssuePage("PROJ", 0, 100)
        }
    }

    @Test
    fun `should use search endpoint for api v3`() = runTest {
        val mockEngine = MockEngine { request ->
            request.url.encodedPath shouldBe "/rest/api/3/search/jql"
            respond(
                content = ByteReadChannel("""{"issues":[]}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        clientFor(mockEngine).fetchIssuePage("PROJ", 0, 100)
    }

    @Test
    fun `should use search endpoint for api v2`() = runTest {
        val mockEngine = MockEngine { request ->
            request.url.encodedPath shouldBe "/rest/api/2/search"
            respond(
                content = ByteReadChannel("""{"issues":[]}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        JiraApiClient(v2Credentials, createMockHttpClient(mockEngine)).fetchIssuePage("PROJ", 0, 100)
    }

    @Test
    fun `should send Bearer header for PAT credentials`() = runTest {
        val mockEngine = MockEngine { request ->
            request.headers["Authorization"] shouldBe "Bearer my-pat-token"
            respond(
                content = ByteReadChannel("""{"issues":[]}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        JiraApiClient(patCredentials, createMockHttpClient(mockEngine)).fetchIssuePage("PROJ", 0, 100)
    }

    @Test
    fun `should fetch server info without auth`() = runTest {
        val mockEngine = MockEngine { request ->
            // serverInfo should not send an Authorization header
            if (request.headers["Authorization"] != null) error("serverInfo must not use auth")
            respond(
                content = ByteReadChannel("""{"serverTitle":"Acme Jira","version":"1001.0.0","deploymentType":"Cloud","baseUrl":"https://acme.atlassian.net"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val info = clientFor(mockEngine).fetchServerInfo()

        info.serverTitle shouldBe "Acme Jira"
        info.deploymentType shouldBe "Cloud"
    }

    @Test
    fun `should fetch current user`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{"displayName":"Jane Doe","emailAddress":"jane@example.com","active":true}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val user = clientFor(mockEngine).fetchCurrentUser()

        user.displayName shouldBe "Jane Doe"
        user.emailAddress shouldBe "jane@example.com"
        user.active shouldBe true
    }

    @Test
    fun `should fetch accessible projects`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""[{"key":"PROJ","name":"My Project"},{"key":"OTHER","name":"Other Project"}]"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val projects = clientFor(mockEngine).fetchProjects()

        projects.size shouldBe 2
        projects[0].key shouldBe "PROJ"
        projects[0].name shouldBe "My Project"
    }

    @Test
    fun `should throw generic Exception on unexpected status`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        assertFailsWith<Exception> { clientFor(mockEngine).fetchIssuePage("PROJ", 0, 100) }
    }
}
