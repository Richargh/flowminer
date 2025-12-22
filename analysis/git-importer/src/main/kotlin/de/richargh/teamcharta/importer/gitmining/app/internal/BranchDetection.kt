package de.richargh.teamcharta.importer.gitmining.app.internal

import de.richargh.teamcharta.importer.git.app.api.*
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import de.richargh.teamcharta.importer.gitmining.app.api.Branch
import de.richargh.teamcharta.importer.gitmining.app.api.Branches
import de.richargh.teamcharta.importer.gitmining.app.api.BranchStatus
import java.time.ZonedDateTime

fun extractBranchInfo(commits: List<Commit>, currentDate: ZonedDateTime): Branches {
    val collector = BranchCollector(commits, currentDate)
    collector.processCommits()

    return collector.toBranches()
}

private class BranchCollector(private val allCommits: List<Commit>, private val currentDate: ZonedDateTime) {
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
            MutableBranch(commit.hash, commit.date, commit.hash, commit.date, commit.branchId, commit.isOnCurrentBranch)
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
        return Branches(branches.values.map { it.toBranch(currentDate) })
    }
}

private class MutableBranch(
    firstCommitHash: CommitHash,
    firstCommitDate: ZonedDateTime,
    lastCommitHash: CommitHash,
    lastCommitDate: ZonedDateTime,
    val branchId: BranchId,
    val isCurrent: Boolean
) {
    var firstCommitHash: CommitHash = firstCommitHash
        private set
    var firstCommitDate: ZonedDateTime = firstCommitDate
        private set
    var lastCommitHash: CommitHash = lastCommitHash
        private set
    var lastCommitDate: ZonedDateTime = lastCommitDate
        private set
    var mergeCommitHash: CommitHash? = null
        private set
    var mergeCommitDate: ZonedDateTime? = null
        private set
    var targetBranch: BranchName? = null
        private set
    var mergedParentHash: CommitHash? = null
        private set

    private var commits = mutableSetOf(firstCommitHash, lastCommitHash)

    fun addCommit(hash: CommitHash, date: ZonedDateTime) {
        commits.add(hash)

        if (date < firstCommitDate) {
            firstCommitHash = hash
            firstCommitDate = date
        }
    }

    fun mergeCommit(hash: CommitHash, date: ZonedDateTime, target: BranchName?, parentHash: CommitHash) {
        mergeCommitHash = hash
        mergeCommitDate = date
        targetBranch = target
        mergedParentHash = parentHash
    }

    fun toBranch(currentDate: ZonedDateTime): Branch {
        val wasMerged = mergeCommitHash != null
        val noCommitsAfterMerge = mergedParentHash?.let { lastCommitHash == it } ?: false
        val isCompleted = wasMerged && noCommitsAfterMerge && !isCurrent

        val threeMonthsAgo = currentDate.minusMonths(3)
        val isActive = !lastCommitDate.isBefore(threeMonthsAgo)

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
