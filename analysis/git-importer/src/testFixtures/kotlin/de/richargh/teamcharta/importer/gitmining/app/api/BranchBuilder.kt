package de.richargh.teamcharta.importer.gitmining.app.api

import de.richargh.teamcharta.importer.git.app.api.*
import java.time.ZonedDateTime

class BranchBuilder {
    private var name: BranchNameCertainty = NamedBranch.Certain(BranchName("feature-branch"))
    private var intermediateCommits = mutableSetOf<CommitHash>()
    private var firstCommitHash: CommitHash = CommitHash("abc123")
    private var firstCommitDate: ZonedDateTime = ZonedDateTime.parse("2024-01-10T10:00:00+01:00")
    private var lastCommitHash: CommitHash? = null
    private var lastCommitDate: ZonedDateTime? = null
    private var mergeCommitHash: CommitHash? = null
    private var mergeDate: ZonedDateTime? = null
    private var targetBranch: BranchName? = null
    private var isCompleted: Boolean = false
    private var isCurrent: Boolean = false
    private var isActive: Boolean = true
    private var isStale: Boolean = false

    fun name(name: String) = apply { this.name = NamedBranch.Certain(BranchName(name)) }
    fun inferredName(name: String) = apply { this.name = NamedBranch.Inferred(BranchName(name)) }
    fun unNamed() = apply { this.name = NamelessBranch }

    fun intermediateCommits(vararg commits: CommitHash) = apply { commits.forEach(this.intermediateCommits::add) }

    fun firstCommitHash(hash: CommitHash) = apply { this.firstCommitHash = hash }
    fun firstCommitDate(date: ZonedDateTime) = apply { this.firstCommitDate = date }
    fun lastCommitHash(hash: CommitHash) = apply { this.lastCommitHash = hash }
    fun lastCommitDate(date: ZonedDateTime) = apply { this.lastCommitDate = date }
    fun mergeCommitHash(hash: CommitHash) = apply { this.mergeCommitHash = hash }
    fun mergeDate(date: ZonedDateTime) = apply { this.mergeDate = date }
    fun targetBranch(branch: String) = apply { this.targetBranch = BranchName(branch) }

    fun mergedInto(targetBranch: String, mergeDate: ZonedDateTime, mergeCommitHash: String) = apply {
        this.mergeCommitHash = CommitHash(mergeCommitHash)
        this.mergeDate = mergeDate
        this.targetBranch = BranchName(targetBranch)
    }

    fun isCompleted() = apply { this.isCompleted = true }
    fun isCurrent() = apply { this.isCurrent = true }
    fun isActive() = apply { this.isActive = true; this.isStale = false }
    fun isStale() = apply { this.isStale = true; this.isActive = false }

    fun build(): Branch = Branch(
        branchNameCertainty = name,
        commits = allCommits(),
        firstCommitHash = firstCommitHash,
        firstCommitDate = firstCommitDate,
        lastCommitHash = lastCommitHash ?: firstCommitHash,
        lastCommitDate = lastCommitDate ?: firstCommitDate,
        mergeCommitHash = mergeCommitHash,
        mergeDate = mergeDate,
        targetBranch = targetBranch,
        isCompleted = isCompleted,
        isCurrent = isCurrent,
        isActive = isActive,
        isStale = isStale
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
