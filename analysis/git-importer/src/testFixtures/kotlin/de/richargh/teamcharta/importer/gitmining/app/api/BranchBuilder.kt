package de.richargh.teamcharta.importer.gitmining.app.api

import de.richargh.teamcharta.importer.git.app.api.*
import kotlin.time.Instant

class BranchBuilder {
    private var name: BranchId = NamedBranchId.Certain(BranchName("feature-branch"))
    private var intermediateCommits = mutableSetOf<CommitHash>()
    private var firstCommitHash: CommitHash = CommitHash("abc123")
    private var firstCommitDate: Instant = Instant.parse("2024-01-10T10:00:00+01:00")
    private var lastCommitHash: CommitHash? = null
    private var lastCommitDate: Instant? = null
    private var mergeCommitHash: CommitHash? = null
    private var mergeDate: Instant? = null
    private var targetBranch: BranchName? = null
    private var isCurrent: Boolean = false
    private var status: BranchStatus = BranchStatus.Active

    fun name(name: String) = apply { this.name = NamedBranchId.Certain(BranchName(name)) }
    fun inferredName(name: String) = apply { this.name = NamedBranchId.Inferred(BranchName(name)) }
    fun unNamed(tipCommit: String = "unknown") = apply { this.name = NamelessBranchId(CommitHash(tipCommit)) }

    fun intermediateCommits(vararg commits: CommitHash) = apply { commits.forEach(this.intermediateCommits::add) }

    fun firstCommitHash(hash: CommitHash) = apply { this.firstCommitHash = hash }
    fun firstCommitDate(date: Instant) = apply { this.firstCommitDate = date }
    fun lastCommitHash(hash: CommitHash) = apply { this.lastCommitHash = hash }
    fun lastCommitDate(date: Instant) = apply { this.lastCommitDate = date }
    fun mergeCommitHash(hash: CommitHash) = apply { this.mergeCommitHash = hash }
    fun mergeDate(date: Instant) = apply { this.mergeDate = date }
    fun targetBranch(branch: String) = apply { this.targetBranch = BranchName(branch) }

    fun mergedInto(targetBranch: String, mergeDate: Instant, mergeCommitHash: String) = apply {
        this.mergeCommitHash = CommitHash(mergeCommitHash)
        this.mergeDate = mergeDate
        this.targetBranch = BranchName(targetBranch)
    }

    fun isCurrent() = apply { this.isCurrent = true }
    fun status(status: BranchStatus) = apply { this.status = status }
    fun isActive() = apply { this.status = BranchStatus.Active }
    fun isStale() = apply { this.status = BranchStatus.Stale }
    fun isCompleted() = apply { this.status = BranchStatus.Completed }

    fun build(): Branch = Branch(
        branchId = name,
        commits = allCommits(),
        firstCommitHash = firstCommitHash,
        firstCommitDate = firstCommitDate,
        lastCommitHash = lastCommitHash ?: firstCommitHash,
        lastCommitDate = lastCommitDate ?: firstCommitDate,
        mergeCommitHash = mergeCommitHash,
        mergeDate = mergeDate,
        targetBranch = targetBranch,
        isCurrent = isCurrent,
        status = status
    )

    private fun allCommits(): Set<CommitHash>{
        val commits = mutableSetOf<CommitHash>()
        commits.add(firstCommitHash)
        commits.addAll(intermediateCommits)
        lastCommitHash?.let(commits::add)
        return commits
    }
}


fun aBranch(block: BranchBuilder.() -> Unit = {}): Branch =
    BranchBuilder().apply(block).build()
