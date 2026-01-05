package de.richargh.teamcharta.importer.github.app.api

/**
 * Configuration for connecting to the GitHub API.
 *
 * @param token Personal Access Token or GitHub App installation token
 * @param baseUrl Base URL for the GitHub API (default: api.github.com)
 */
data class GitHubCredentials(
    val token: String,
    val baseUrl: String = "https://api.github.com"
)
