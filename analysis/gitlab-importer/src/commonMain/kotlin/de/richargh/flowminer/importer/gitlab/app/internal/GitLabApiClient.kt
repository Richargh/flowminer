package de.richargh.flowminer.importer.gitlab.app.internal

import de.richargh.flowminer.importer.gitlab.app.api.GitLabAuthException
import de.richargh.flowminer.importer.gitlab.app.api.GitLabCredentials
import de.richargh.flowminer.importer.gitlab.app.api.GitLabNotFoundException
import de.richargh.flowminer.importer.gitlab.app.api.GitLabRateLimitException
import de.richargh.flowminer.importer.gitlab.app.dto.GitLabJobDto
import de.richargh.flowminer.importer.gitlab.app.dto.GitLabPipelineDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private fun encodeProjectId(projectId: String) = projectId.replace("/", "%2F")

private fun HttpResponse.checkStatus(projectId: String) {
    when (status) {
        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> throw GitLabAuthException()
        HttpStatusCode.NotFound -> throw GitLabNotFoundException(projectId)
        HttpStatusCode.TooManyRequests -> throw GitLabRateLimitException()
        else -> if (!status.isSuccess()) throw Exception("Unexpected GitLab API response: $status")
    }
}

class GitLabApiClient(
    private val credentials: GitLabCredentials,
    private val httpClient: HttpClient
) {
    suspend fun fetchRecentPipelines(projectId: String, count: Int = 10): List<GitLabPipelineDto> {
        val encodedId = encodeProjectId(projectId)
        val url = "${credentials.host}/api/v4/projects/$encodedId/pipelines?per_page=$count&order_by=id&sort=desc"

        val response = httpClient.get(url) {
            header("PRIVATE-TOKEN", credentials.token)
        }
        response.checkStatus(projectId)
        return response.body()
    }

    suspend fun fetchJobsForPipeline(projectId: String, pipelineId: Int): List<GitLabJobDto> {
        val encodedId = encodeProjectId(projectId)
        val firstPageUrl = "${credentials.host}/api/v4/projects/$encodedId/pipelines/$pipelineId/jobs?per_page=100"
        return fetchAllPages(firstPageUrl)
    }

    private suspend fun fetchAllPages(url: String): List<GitLabJobDto> {
        val allJobs = mutableListOf<GitLabJobDto>()
        var nextUrl: String? = url
        val projectId = url.substringAfter("/projects/").substringBefore("/pipelines")

        while (nextUrl != null) {
            val response = httpClient.get(nextUrl) {
                header("PRIVATE-TOKEN", credentials.token)
            }
            response.checkStatus(projectId)

            val jobs: List<GitLabJobDto> = response.body()
            allJobs.addAll(jobs)

            nextUrl = parseLinkHeader(response.headers["Link"])
        }

        return allJobs
    }

    fun close() {
        httpClient.close()
    }
}

internal fun parseLinkHeader(linkHeader: String?): String? {
    if (linkHeader == null) return null
    return linkHeader.split(",")
        .map { it.trim() }
        .firstOrNull { it.contains("""rel="next"""") }
        ?.let { linkPart ->
            val urlMatch = Regex("""<([^>]+)>""").find(linkPart)
            urlMatch?.groupValues?.get(1)
        }
}

internal fun createGitLabHttpClient(): HttpClient {
    return createHttpClient().config {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
}
