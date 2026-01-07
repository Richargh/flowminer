package de.richargh.flowminer.importer.github.app

import de.richargh.flowminer.importer.github.app.api.GitHubCredentials
import de.richargh.flowminer.importer.github.app.api.GitHubWorkItem
import de.richargh.flowminer.importer.github.app.api.RepositoryId
import de.richargh.flowminer.importer.github.app.internal.GitHubGraphQlClient
import de.richargh.flowminer.importer.github.app.internal.createGraphQLHttpClient
import de.richargh.flowminer.importer.github.app.internal.toGitHubWorkItem
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Service for fetching GitHub issues as work items.
 *
 * This service uses the GraphQL API to fetch all issues with:
 * - Full timeline events (no N+1 problem)
 * - Native parent/subIssues hierarchy
 * - Labels, assignees, milestones
 *
 * This is the main public API for the github-importer library.
 */
class GitHubImporter(
    credentials: GitHubCredentials,
    httpClient: HttpClient = createGraphQLHttpClient()
) {
    private val graphqlClient = GitHubGraphQlClient(credentials, httpClient)
    /**
     * Fetch all issues from a repository with full timeline events.
     *
     * Uses a single GraphQL query per page of issues, eliminating the N+1 problem
     * that existed in the REST API implementation.
     *
     * @param repoId The repository to fetch issues from
     * @param quiet If true, suppress progress logging
     * @return List of GitHubWorkItem with transitions populated
     */
    fun fetchIssuesWithTimelines(repoId: RepositoryId, quiet: Boolean = false): Flow<GitHubWorkItem> {
        return graphqlClient.fetchAllIssues(repoId, quiet).map { it.toGitHubWorkItem(repoId) }
    }

    /**
     * Close the underlying HTTP client.
     */
    fun close() {
        graphqlClient.close()
    }
}