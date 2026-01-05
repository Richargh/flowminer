package de.richargh.teamcharta.importer.github.app.api

import kotlin.time.Instant

/**
 * Thrown when the GitHub API rate limit has been exceeded.
 *
 * @param resetAt When the rate limit will reset
 * @param remaining Number of requests remaining (should be 0)
 */
class RateLimitExceededException(
    val resetAt: Instant,
    val remaining: Int = 0
) : RuntimeException("GitHub API rate limit exceeded. Resets at: $resetAt")

/**
 * Thrown when the requested repository is not found (404).
 * This can happen for non-existent repos or private repos without proper access.
 */
class RepositoryNotFoundException(
    val owner: String,
    val repo: String
) : RuntimeException("Repository not found: $owner/$repo. Check if it exists and you have access.")
