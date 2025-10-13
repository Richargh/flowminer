
package de.richargh.teamcharta.importer.git.app.api

import java.time.Instant

/**
 * Base interface for all git lifecycle events.
 */
sealed interface GitEvent {
    val timestamp: Instant
    val commitHash: String
    val branchName: String
}

/**
 * Event representing the creation of a new branch.
 * Typically identified when a commit is the first on a branch that diverges from another.
 */
data class BranchCreated(
    override val timestamp: Instant,
    override val commitHash: String,
    override val branchName: String,
    val parentBranch: String?,
    val author: String
) : GitEvent

/**
 * Event representing a commit made on a branch.
 */
data class CommitMade(
    override val timestamp: Instant,
    override val commitHash: String,
    override val branchName: String,
    val author: String,
    val message: String
) : GitEvent

/**
 * Event representing when a branch is merged back to the default branch.
 * Identified by merge commits in the git history.
 */
data class BranchMerged(
    override val timestamp: Instant,
    override val commitHash: String,
    override val branchName: String,
    val targetBranch: String,
    val author: String,
    val mergeMessage: String
) : GitEvent

/**
 * Container for a series of git events, representing the complete lifecycle
 * of branches and commits in a repository.
 */
data class GitEventSeries(
    val events: List<GitEvent>,
    val defaultBranch: String
)