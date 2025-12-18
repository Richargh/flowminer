package de.richargh.teamcharta.importer.gitmining.app.api

import de.richargh.teamcharta.importer.git.app.api.Author
import de.richargh.teamcharta.importer.git.app.api.BranchName
import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import de.richargh.teamcharta.importer.git.app.api.CommitType
import de.richargh.teamcharta.importer.git.app.api.BranchNameCertainty
import de.richargh.teamcharta.importer.git.app.api.NamedBranch
import de.richargh.teamcharta.importer.git.app.api.NamelessBranch
import de.richargh.teamcharta.importer.git.app.api.WorkKey
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime

enum class BranchStatus {
    Active,
    Stale,
    Completed
}

data class Branch(
    val branchNameCertainty: BranchNameCertainty,
    val commits: Set<CommitHash>,
    val firstCommitHash: CommitHash,
    val firstCommitDate: ZonedDateTime,
    val lastCommitHash: CommitHash,
    val lastCommitDate: ZonedDateTime,
    val mergeCommitHash: CommitHash?,
    val mergeDate: ZonedDateTime?,
    val targetBranch: BranchName?,
    val isCurrent: Boolean,
    val status: BranchStatus
) {
    val name: BranchName? get() = when (branchNameCertainty) {
        is NamedBranch.Certain -> branchNameCertainty.name
        is NamedBranch.Inferred -> branchNameCertainty.name
        is NamelessBranch -> null
    }

    val isNamed: Boolean get() = branchNameCertainty is NamedBranch
    val isUnnamed: Boolean get() = branchNameCertainty is NamelessBranch
    val isInferred: Boolean get() = branchNameCertainty is NamedBranch.Inferred

    val age: Duration get() = Duration.between(firstCommitDate, lastCommitDate)
}

class Commits(private val commits: List<Commit>) {
    operator fun get(index: Int): Commit = commits[index]

    fun all(): Collection<Commit> = commits
    fun first(): Commit = commits.first()
    fun size(): Int = commits.size
    fun isEmpty(): Boolean = commits.isEmpty()
    fun isNotEmpty(): Boolean = commits.isNotEmpty()
}

class Branches(branches: List<Branch>) {
    private val namedBranches: Map<BranchName, Branch> = branches
        .filter { it.isNamed }
        .associateBy { it.name!! }

    val unnamed: List<Branch> = branches.filter { it.isUnnamed }

    operator fun get(name: BranchName): Branch? = namedBranches[name]
    operator fun get(name: String): Branch? = namedBranches[BranchName(name)]

    fun all(): Collection<Branch> = namedBranches.values + unnamed
    fun size() = namedBranches.size + unnamed.size
}

data class AuthorContribution(
    val author: Author,
    val linesChanged: Int
)

data class WorkItem(
    val workKey: WorkKey,
    val linesAdded: Int,
    val linesRemoved: Int,
    val firstCommitDate: ZonedDateTime,
    val lastCommitDate: ZonedDateTime,
    val filesChanged: Set<Path>,
    val contributions: List<AuthorContribution>,
    val absoluteChurnByType: Map<CommitType, Int>,
    val commits: Int,
    val collaborators: Int,
    val reworkFiles: Set<Path>
) {
    val duration: Duration get() = Duration.between(firstCommitDate, lastCommitDate)
}

class WorkItems(workItems: List<WorkItem>) {
    private val workItems: Map<WorkKey, WorkItem> = workItems
        .associateBy { it.workKey }

    fun all(): Collection<WorkItem> = workItems.values
    fun isEmpty(): Boolean = workItems.isEmpty()
    fun isNotEmpty(): Boolean = workItems.isNotEmpty()
    fun size(): Int = workItems.size

    // TODO val unnamed: List<WorkItem> = workItems.filter { it.isUnnamed }

    operator fun get(key: WorkKey): WorkItem? = workItems[key]
    operator fun get(key: String): WorkItem? = workItems[WorkKey.Known(key)]
}

data class ChurnMetric(
    val additions: Int,
    val deletions: Int
) {
    operator fun plus(other: ChurnMetric): ChurnMetric =
        ChurnMetric(additions + other.additions, deletions + other.deletions)
}

data class AuthorStatistic(
    val author: Author,
    val commitCount: Int,
    val linesAdded: Int,
    val linesRemoved: Int,
    val workItems: Set<WorkKey> = emptySet(),
    val churnByCommitType: Map<CommitType, ChurnMetric> = emptyMap(),
    val collaborators: Set<Author> = emptySet()
)

class AuthorStatistics(authorStatistics: List<AuthorStatistic>) {
    private val authorStatistics: Map<Author, AuthorStatistic> = authorStatistics
        .associateBy { it.author }

    fun all(): Collection<AuthorStatistic> = authorStatistics.values
    fun size(): Int = authorStatistics.size

    operator fun get(key: Author): AuthorStatistic? = authorStatistics[key]
}

data class GitMiningResult(
    val commits: Commits,
    val branches: Branches = Branches(emptyList()),
    val workItems: WorkItems = WorkItems(emptyList()),
    val authorStatistics: AuthorStatistics = AuthorStatistics(emptyList())
)