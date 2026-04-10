package de.richargh.flowminer.importer.gitlab.app.internal

import de.richargh.flowminer.importer.gitlab.app.api.GitLabAuthException
import de.richargh.flowminer.importer.gitlab.app.api.GitLabCredentials
import de.richargh.flowminer.importer.gitlab.app.api.GitLabNotFoundException
import de.richargh.flowminer.importer.gitlab.app.api.GitLabRateLimitException
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

private fun createMockHttpClient(mockEngine: MockEngine): HttpClient {
    return HttpClient(mockEngine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
}

class GitLabApiClientTest {

    private val credentials = GitLabCredentials(
        token = "test-token",
        host = "https://gitlab.example.com"
    )

    @Test
    fun `shouldFetchPipelines`() = runTest {
        // given
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""[{"id":1,"ref":"main","status":"success","created_at":"2024-01-01T10:00:00.000Z"},{"id":2,"ref":"feature/foo","status":"failed","created_at":"2024-01-02T10:00:00.000Z"}]"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = GitLabApiClient(credentials, createMockHttpClient(mockEngine))

        // when
        val pipelines = client.fetchRecentPipelines("my/project", count = 10)

        // then
        pipelines.size shouldBe 2
        pipelines[0].id shouldBe 1
        pipelines[1].id shouldBe 2
    }

    @Test
    fun `shouldFetchJobsForPipeline`() = runTest {
        // given
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""[{"id":10,"name":"build","stage":"build","status":"success","started_at":"2024-01-01T10:01:00.000Z","finished_at":"2024-01-01T10:05:00.000Z","duration":240.0,"allow_failure":false},{"id":11,"name":"test","stage":"test","status":"success","started_at":"2024-01-01T10:06:00.000Z","finished_at":"2024-01-01T10:10:00.000Z","duration":240.0,"allow_failure":false},{"id":12,"name":"deploy","stage":"deploy","status":"success","started_at":"2024-01-01T10:11:00.000Z","finished_at":"2024-01-01T10:12:00.000Z","duration":60.0,"allow_failure":false}]"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = GitLabApiClient(credentials, createMockHttpClient(mockEngine))

        // when
        val jobs = client.fetchJobsForPipeline("my/project", pipelineId = 1)

        // then
        jobs.size shouldBe 3
        jobs[0].id shouldBe 10
        jobs[1].id shouldBe 11
        jobs[2].id shouldBe 12
    }

    @Test
    fun `shouldFollowPaginationForJobs`() = runTest {
        // given
        var requestCount = 0
        val mockEngine = MockEngine { _ ->
            requestCount++
            if (requestCount == 1) {
                respond(
                    content = ByteReadChannel("""[{"id":10,"name":"build","stage":"build","status":"success","allow_failure":false},{"id":11,"name":"test","stage":"test","status":"success","allow_failure":false}]"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf("application/json"),
                        HttpHeaders.Link to listOf("""<https://gitlab.example.com/api/v4/projects/my%2Fproject/pipelines/1/jobs?page=2&per_page=100>; rel="next"""")
                    )
                )
            } else {
                respond(
                    content = ByteReadChannel("""[{"id":12,"name":"deploy","stage":"deploy","status":"success","allow_failure":false},{"id":13,"name":"release","stage":"release","status":"success","allow_failure":false}]"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        val client = GitLabApiClient(credentials, createMockHttpClient(mockEngine))

        // when
        val jobs = client.fetchJobsForPipeline("my/project", pipelineId = 1)

        // then
        jobs.size shouldBe 4
    }

    @Test
    fun `shouldSurfaceRateLimitError`() = runTest {
        // given
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = GitLabApiClient(credentials, createMockHttpClient(mockEngine))

        // when / then
        var exceptionThrown = false
        try {
            client.fetchRecentPipelines("my/project", count = 10)
        } catch (e: GitLabRateLimitException) {
            exceptionThrown = true
        }
        exceptionThrown shouldBe true
    }

    @Test
    fun `shouldThrowAuthExceptionOn401`() = runTest {
        // given
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{"message":"401 Unauthorized"}"""),
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = GitLabApiClient(credentials, createMockHttpClient(mockEngine))

        // when / then
        var exceptionThrown = false
        try {
            client.fetchRecentPipelines("my/project", count = 10)
        } catch (e: GitLabAuthException) {
            exceptionThrown = true
        }
        exceptionThrown shouldBe true
    }

    @Test
    fun `shouldThrowNotFoundExceptionOn404`() = runTest {
        // given
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{"message":"404 Project Not Found"}"""),
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = GitLabApiClient(credentials, createMockHttpClient(mockEngine))

        // when / then
        var exceptionThrown = false
        try {
            client.fetchRecentPipelines("my/project", count = 10)
        } catch (e: GitLabNotFoundException) {
            exceptionThrown = true
        }
        exceptionThrown shouldBe true
    }
}
