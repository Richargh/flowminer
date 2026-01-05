package de.richargh.teamcharta.importer.github.app

import de.richargh.teamcharta.importer.github.app.api.GitHubWorkItem
import de.richargh.teamcharta.importer.github.app.api.RepositoryId
import de.richargh.teamcharta.importer.github.app.internal.GitHubGraphQlClient
import de.richargh.teamcharta.importer.github.app.internal.toGitHubWorkItem

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
    private val graphqlClient: GitHubGraphQlClient
) {
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
    suspend fun fetchIssuesWithTimelines(repoId: RepositoryId, quiet: Boolean = false): List<GitHubWorkItem> {
        val graphqlIssues = graphqlClient.fetchAllIssues(repoId, quiet)

        if (!quiet) println("Converting ${graphqlIssues.size} issues...")
        val workItems = graphqlIssues.map { it.toGitHubWorkItem(repoId) }
        return workItems
    }

    /**
     * Close the underlying HTTP client.
     */
    fun close() {
        graphqlClient.close()
    }
}