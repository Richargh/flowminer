package de.richargh.flowminer.importer.gitlab.app

import de.richargh.flowminer.importer.gitlab.app.api.GitLabCredentials
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

class GitLabCiImporterTest {

    private val credentials = GitLabCredentials(
        token = "test-token",
        host = "https://gitlab.example.com"
    )

    @Test
    fun `shouldEmitProgressMessagesWithPipelineDetails`() = runTest {
        // given - 2 pipelines, 2 jobs each
        val pipelinesJson = """[{"id":1,"ref":"main","status":"success","created_at":"2024-01-01T10:00:00.000Z"},{"id":2,"ref":"feature","status":"success","created_at":"2024-01-02T10:00:00.000Z"}]"""
        val jobsJson = """[{"id":10,"name":"build","stage":"build","status":"success","started_at":"2024-01-01T10:01:00.000Z","finished_at":"2024-01-01T10:05:00.000Z","duration":240.0,"allow_failure":false}]"""
        var callCount = 0
        val mockEngine = MockEngine { _ ->
            callCount++
            respond(ByteReadChannel(if (callCount == 1) pipelinesJson else jobsJson), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val importer = GitLabCiImporter(credentials, httpClient)
        val logMessages = mutableListOf<String>()

        // when
        importer.import("my/project", log = { logMessages += it })

        // then
        logMessages.any { "my/project" in it } shouldBe true
        logMessages.any { "#1" in it && "main" in it } shouldBe true
        logMessages.any { "#2" in it && "feature" in it } shouldBe true
    }

    @Test
    fun `shouldFetchPipelinesAndJobs`() = runTest {
        // given - 2 pipelines, each with 2 jobs
        val pipelinesJson = """[{"id":1,"ref":"main","status":"success","created_at":"2024-01-01T10:00:00.000Z"},{"id":2,"ref":"main","status":"success","created_at":"2024-01-02T10:00:00.000Z"}]"""
        val jobsJson1 = """[{"id":10,"name":"build","stage":"build","status":"success","started_at":"2024-01-01T10:01:00.000Z","finished_at":"2024-01-01T10:05:00.000Z","duration":240.0,"allow_failure":false},{"id":11,"name":"test","stage":"test","status":"success","started_at":"2024-01-01T10:06:00.000Z","finished_at":"2024-01-01T10:10:00.000Z","duration":240.0,"allow_failure":false}]"""
        val jobsJson2 = """[{"id":20,"name":"build","stage":"build","status":"success","started_at":"2024-01-02T10:01:00.000Z","finished_at":"2024-01-02T10:05:00.000Z","duration":240.0,"allow_failure":false},{"id":21,"name":"test","stage":"test","status":"success","started_at":"2024-01-02T10:06:00.000Z","finished_at":"2024-01-02T10:10:00.000Z","duration":240.0,"allow_failure":false}]"""

        var callCount = 0
        val mockEngine = MockEngine { _ ->
            callCount++
            when (callCount) {
                1 -> respond(ByteReadChannel(pipelinesJson), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                2 -> respond(ByteReadChannel(jobsJson1), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond(ByteReadChannel(jobsJson2), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val importer = GitLabCiImporter(credentials, httpClient)

        // when
        val jobs = importer.import("my/project")

        // then
        jobs.size shouldBe 4
        jobs[0].pipelineId.value shouldBe 1
        jobs[0].jobName.value shouldBe "build"
        jobs[2].pipelineId.value shouldBe 2
    }
}
