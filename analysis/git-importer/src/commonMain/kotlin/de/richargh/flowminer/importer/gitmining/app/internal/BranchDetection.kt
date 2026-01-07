package de.richargh.flowminer.importer.gitmining.app.internal

import de.richargh.flowminer.importer.git.app.api.*
import de.richargh.flowminer.importer.git.app.api.CommitHash
import de.richargh.flowminer.importer.gitmining.app.api.Branch
import de.richargh.flowminer.importer.gitmining.app.api.Branches
import de.richargh.flowminer.importer.gitmining.app.api.BranchStatus
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

fun extractBranchInfo(commits: List<Commit>, currentDate: Instant): Branches {
    val collector =
        BranchCollector(commits, currentDate)
    collector.processCommits()

    return collector.toBranches()
}

private class BranchCollector(private val allCommits: List<Commit>, private val currentDate: Instant) {
    private val commitByHash = mutableMapOf<CommitHash, Commit>()
    private val branches = mutableMapOf<BranchId, MutableBranch>()

    fun processCommits() {
        val pendingMerges = mutableListOf<Commit>()

        for (commit in allCommits) {
            commitByHash[commit.hash] = commit
            addToBranch(commit)

            if (commit.isMerge) {
                pendingMerges.add(commit)
            }
        }

        for (mergeCommit in pendingMerges) {
            processMerges(mergeCommit)
        }
    }

    private fun addToBranch(commit: Commit) {
        val branch = branches.getOrPut(commit.branchId) {
            MutableBranch(
                commit.hash,
                commit.date,
                commit.hash,
                commit.date,
                commit.branchId,
                commit.isOnCurrentBranch
            )
        }
        branch.addCommit(commit.hash, commit.date)
    }

    private fun processMerges(commit: Commit) {
        commit.parents.drop(1).forEach { featureBranchHash ->
            // Don't mark the current branch as merged when main is merged into a feature branch
            val featureBranchCommit = commitByHash[featureBranchHash]
            if (featureBranchCommit?.isOnCurrentBranch == true) return@forEach

            val featureBranchId = featureBranchCommit!!.branchId
            val featureBranch = branches[featureBranchId]!!
            if (featureBranch.mergeCommitHash == null) {
                featureBranch.mergeCommit(commit.hash, commit.date, commit.branchId.name, featureBranchHash)
            }
        }
    }

    fun toBranches(): Branches {
        return Branches(branches.values.map {
            it.toBranch(
                currentDate
            )
        })
    }
}

private class MutableBranch(
    firstCommitHash: CommitHash,
    firstCommitDate: Instant,
    lastCommitHash: CommitHash,
    lastCommitDate: Instant,
    val branchId: BranchId,
    val isCurrent: Boolean
) {
    var firstCommitHash: CommitHash = firstCommitHash
        private set
    var firstCommitDate: Instant = firstCommitDate
        private set
    var lastCommitHash: CommitHash = lastCommitHash
        private set
    var lastCommitDate: Instant = lastCommitDate
        private set
    var mergeCommitHash: CommitHash? = null
        private set
    var mergeCommitDate: Instant? = null
        private set
    var targetBranch: BranchName? = null
        private set
    var mergedParentHash: CommitHash? = null
        private set

    private var commits = mutableSetOf(firstCommitHash, lastCommitHash)

    fun addCommit(hash: CommitHash, date: Instant) {
        commits.add(hash)

        if (date < firstCommitDate) {
            firstCommitHash = hash
            firstCommitDate = date
        }
    }

    fun mergeCommit(hash: CommitHash, date: Instant, target: BranchName?, parentHash: CommitHash) {
        mergeCommitHash = hash
        mergeCommitDate = date
        targetBranch = target
        mergedParentHash = parentHash
    }

    fun toBranch(currentDate: Instant): Branch {
        val wasMerged = mergeCommitHash != null
        val noCommitsAfterMerge = mergedParentHash?.let { lastCommitHash == it } ?: false
        val isCompleted = wasMerged && noCommitsAfterMerge && !isCurrent

        val threeMonthsAgo = currentDate - 90.days
        val isActive = lastCommitDate >= threeMonthsAgo

        val status = when {
            isCompleted -> BranchStatus.Completed
            isActive -> BranchStatus.Active
            else -> BranchStatus.Stale
        }

        return Branch(
            branchId = branchId,
            commits = commits,
            firstCommitHash = firstCommitHash,
            firstCommitDate = firstCommitDate,
            lastCommitHash = lastCommitHash,
            lastCommitDate = lastCommitDate,
            mergeCommitHash = mergeCommitHash,
            mergeDate = mergeCommitDate,
            targetBranch = targetBranch,
            isCurrent = isCurrent,
            status = status
        )
    }
}
