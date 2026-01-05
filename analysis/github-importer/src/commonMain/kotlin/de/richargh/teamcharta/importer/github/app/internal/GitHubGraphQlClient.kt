package de.richargh.teamcharta.importer.github.app.internal

import de.richargh.teamcharta.importer.github.app.api.GitHubCredentials
import de.richargh.teamcharta.importer.github.app.api.RateLimitExceededException
import de.richargh.teamcharta.importer.github.app.api.RepositoryId
import de.richargh.teamcharta.importer.github.app.api.RepositoryNotFoundException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * GraphQL request body with query and variables.
 */
@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, String> = emptyMap()
)

/**
 * Builds the GraphQL query for fetching issues with all related data.
 */
fun buildIssuesQuery(repoId: RepositoryId, cursor: String?): GraphQLRequest {
    val variables = mutableMapOf(
        "owner" to repoId.owner,
        "name" to repoId.name
    )
    if (cursor != null) {
        variables["cursor"] = cursor
    }

    val query = """
        query(${"$"}owner: String!, ${"$"}name: String!, ${"$"}cursor: String) {
          repository(owner: ${"$"}owner, name: ${"$"}name) {
            issues(first: 100, states: [OPEN, CLOSED], after: ${"$"}cursor) {
              pageInfo { hasNextPage endCursor }
              nodes {
                number title state body createdAt closedAt
                author { login }
                labels(first: 100) { pageInfo { hasNextPage } nodes { name color } }
                assignees(first: 100) { pageInfo { hasNextPage } nodes { login } }
                milestone { title state }
                parent { number repository { owner { login } name } }
                subIssues(first: 100) { pageInfo { hasNextPage } nodes { number } }
                timelineItems(first: 100) {
                  pageInfo { hasNextPage }
                  nodes {
                    __typename
                    ... on LabeledEvent { createdAt actor { login } label { name } }
                    ... on UnlabeledEvent { createdAt actor { login } label { name } }
                    ... on AssignedEvent { createdAt actor { login } assignee { ... on User { login } } }
                    ... on UnassignedEvent { createdAt actor { login } assignee { ... on User { login } } }
                    ... on ClosedEvent { createdAt actor { login } }
                    ... on ReopenedEvent { createdAt actor { login } }
                    ... on MilestonedEvent { createdAt actor { login } milestoneTitle }
                    ... on DemilestonedEvent { createdAt actor { login } milestoneTitle }
                  }
                }
              }
            }
          }
        }
    """.trimIndent()

    return GraphQLRequest(query = query, variables = variables)
}

/**
 * GitHub GraphQL API client.
 *
 * Fetches issues with timeline events and hierarchy in a single query.
 */
class GitHubGraphQlClient(
    private val credentials: GitHubCredentials,
    private val httpClient: HttpClient = createGraphQLHttpClient()
) {
    /**
     * Fetch all issues from a repository using GraphQL.
     * Handles pagination automatically.
     *
     * @param repoId The repository to fetch issues from
     * @param quiet If true, suppress progress logging
     */
    fun fetchAllIssues(repoId: RepositoryId, quiet: Boolean = false): Flow<GraphQLIssue> = flow {
        var cursor: String? = null
        var hasNextPage = true
        var page = 0
        var issueCount = 0

        if (!quiet) println("Fetching issues from ${repoId.owner}/${repoId.name}...")

        while (hasNextPage) {
            page++
            val response = executeQuery(repoId, cursor)
            checkForErrors(response, repoId)

            val issues = response.data?.repository?.issues
            if (issues != null) {
                issueCount += issues.nodes.size
                issues.nodes.forEach { emit(it) }
                hasNextPage = issues.pageInfo?.hasNextPage ?: false
                cursor = issues.pageInfo?.endCursor
                if (!quiet) println("  Page $page: $issueCount issues fetched")

                // Warn about nested pagination limits
                checkNestedPaginationLimits(issues.nodes, quiet)
            } else {
                hasNextPage = false
            }
        }
    }

    /**
     * Check for nested pagination limits and warn if any are exceeded.
     * Warnings are always printed (even in quiet mode) since they indicate data loss.
     */
    private fun checkNestedPaginationLimits(issues: List<GraphQLIssue>, quiet: Boolean) {
        for (issue in issues) {
            if (issue.labels?.pageInfo?.hasNextPage == true) {
                println("WARNING: Issue #${issue.number} has more than 100 labels (truncated)")
            }
            if (issue.assignees?.pageInfo?.hasNextPage == true) {
                println("WARNING: Issue #${issue.number} has more than 100 assignees (truncated)")
            }
            if (issue.subIssues?.pageInfo?.hasNextPage == true) {
                println("WARNING: Issue #${issue.number} has more than 100 sub-issues (truncated)")
            }
            if (issue.timelineItems?.pageInfo?.hasNextPage == true) {
                println("WARNING: Issue #${issue.number} has more than 100 timeline events (truncated)")
            }
        }
    }

    /**
     * Execute a GraphQL query and return the parsed response.
     */
    internal suspend fun executeQuery(repoId: RepositoryId, cursor: String?): GraphQLResponse {
        val request = buildIssuesQuery(repoId, cursor)

        val httpResponse = httpClient.post("${credentials.baseUrl}/graphql") {
            header("Authorization", "Bearer ${credentials.token}")
            header("GraphQL-Features", "sub_issues")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (httpResponse.status == HttpStatusCode.Unauthorized) {
            throw IllegalStateException("GitHub API authentication failed. Check your token.")
        }

        if (httpResponse.status == HttpStatusCode.NotFound) {
            throw RepositoryNotFoundException(repoId.owner, repoId.name)
        }

        return httpResponse.body()
    }

    /**
     * Check GraphQL response for errors.
     */
    private fun checkForErrors(response: GraphQLResponse, repoId: RepositoryId) {
        val errors = response.errors
        if (errors.isNullOrEmpty()) return

        val firstError = errors.first()

        // Check for rate limit errors
        if (firstError.type == "RATE_LIMITED" || firstError.message.contains("rate limit", ignoreCase = true)) {
            throw RateLimitExceededException(
                resetAt = Instant.DISTANT_FUTURE,
                remaining = 0
            )
        }

        // Check for not found errors
        if (firstError.type == "NOT_FOUND" || firstError.message.contains("not found", ignoreCase = true)) {
            throw RepositoryNotFoundException(repoId.owner, repoId.name)
        }

        // Generic GraphQL error
        throw IllegalStateException("GraphQL error: ${firstError.message}")
    }

    /**
     * Close the HTTP client.
     */
    fun close() {
        httpClient.close()
    }
}

/**
 * Create a configured HttpClient for GraphQL requests.
 */
internal fun createGraphQLHttpClient(): HttpClient {
    return createHttpClient().config {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
}
