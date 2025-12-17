package de.richargh.teamcharta.importer.gitmining.app.internal

import de.richargh.teamcharta.importer.git.app.api.*
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import de.richargh.teamcharta.importer.gitmining.app.api.Branch
import de.richargh.teamcharta.importer.gitmining.app.api.Branches
import java.time.ZonedDateTime

fun extractBranchInfo(commits: List<Commit>): Branches {
    val collector = BranchCollector(commits)
    collector.processCommits()

    return collector.toBranches()
}

private class BranchCollector(private val allCommits: List<Commit>) {
    private val branchIdByHash = mutableMapOf<CommitHash, BranchId>()
    private val commitByHash = mutableMapOf<CommitHash, Commit>()

    private val branches = mutableMapOf<BranchId, MutableBranch>()

    private val upcomingBranch = mutableMapOf<CommitHash, BranchId>()

    fun processCommits() {
        val pendingMerges = mutableListOf<PendingMerge>()

        for (commit in allCommits) {
            val branchId = lookupBranchId(commit)
            branchIdByHash[commit.hash] = branchId
            commitByHash[commit.hash] = commit

            addToBranch(commit, branchId)

            if (commit.isMerge) {
                pendingMerges.add(PendingMerge(commit, branchId))
            }
        }

        for (pending in pendingMerges) {
            processMerges(pending.commit)
        }
    }

    private fun lookupBranchId(commit: Commit): BranchId {
        var branchId: BranchId = upcomingBranch.remove(commit.hash)
            ?: BranchId.LastCommit(commit.hash)
        if (commit.branch is NamedBranch)
            branchId = BranchId.Name(commit.branch)

        if(commit.parents.isNotEmpty()){
            upcomingBranch[commit.parents[0]] = branchId
        }
        if (commit.isMerge) {
            commit.parents.drop(1).forEach { parentHash ->
                upcomingBranch[parentHash] = BranchId.LastCommit(parentHash)
            }
        }
        return branchId
    }

    private fun addToBranch(commit: Commit, branchId: BranchId) {
        val name = when (branchId) {
            is BranchId.Name -> branchId.name
            is BranchId.LastCommit -> NamelessBranch
        }
        val branch = branches.getOrPut(branchId) {
            MutableBranch(commit.hash, commit.date, commit.hash, commit.date, name, commit.isOnCurrentBranch)
        }
        branch.addCommit(commit.hash, commit.date)
    }

    private fun processMerges(commit: Commit) {
        commit.parents.drop(1).forEach { featureBranchHash ->
            // Don't mark the current branch as merged when main is merged into a feature branch
            val featureBranchCommit = commitByHash[featureBranchHash]
            if (featureBranchCommit?.isOnCurrentBranch == true) return@forEach

            val featureBranchId = branchIdByHash[featureBranchHash]!!
            val featureBranch = branches[featureBranchId]!!
            if (featureBranch.mergeCommitHash == null) {
                featureBranch.mergeCommit(commit.hash, commit.date, commit.branch.name, featureBranchHash)
            }
        }
    }

    fun toBranches(): Branches {
        return Branches(branches.values.map(MutableBranch::toBranch))
    }
}

private sealed interface BranchId {
    data class Name(val name: BranchNameCertainty) : BranchId
    data class LastCommit(val lastCommitHash: CommitHash) : BranchId
}

private data class PendingMerge(val commit: Commit, val branchId: BranchId)

private class MutableBranch(
    firstCommitHash: CommitHash,
    firstCommitDate: ZonedDateTime,
    lastCommitHash: CommitHash,
    lastCommitDate: ZonedDateTime,
    val branchNameCertainty: BranchNameCertainty,
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

    fun toBranch(): Branch {
        val wasMerged = mergeCommitHash != null
        val noCommitsAfterMerge = mergedParentHash?.let { lastCommitHash == it } ?: false
        val isCompleted = wasMerged && noCommitsAfterMerge && !isCurrent

        return Branch(
            branchNameCertainty = branchNameCertainty,
            commits = commits,
            firstCommitHash = firstCommitHash,
            firstCommitDate = firstCommitDate,
            lastCommitHash = lastCommitHash,
            lastCommitDate = lastCommitDate,
            mergeCommitHash = mergeCommitHash,
            mergeDate = mergeCommitDate,
            targetBranch = targetBranch,
            isCompleted = isCompleted,
            isCurrent = isCurrent
        )
    }
}
