package de.richargh.teamcharta.importer.gitmining.app.internal

import de.richargh.teamcharta.importer.git.app.api.*
import de.richargh.teamcharta.importer.gitmining.app.api.Branch
import de.richargh.teamcharta.importer.gitmining.app.api.Branches
import java.time.ZonedDateTime

fun extractBranchInfo(commits: List<Commit>): Branches {
    val collector = BranchCollector(commits)
    collector.processCommits()

    return collector.toBranches()
}

private class BranchCollector(private val commits: List<Commit>) {
    private val branchByHash = commits.associate { it.hash to it.branch }
    private val commitByHash = commits.associateBy { it.hash }
    private val hasActiveBranch = commits.any { it.isOnActiveBranch }

    private val namedBranches = mutableMapOf<BranchName, MutableBranch>()
    private val unnamedBranches = mutableMapOf<CommitHash, MutableBranch>()

    fun processCommits() {
        commits.forEach(::processCommit)
    }

    private fun processCommit(commit: Commit) {
        val commitBranchName: NamedBranch = when (val certainty = commit.branch) {
            is NamelessBranch -> return
            is NamedBranch -> certainty
        }

        updateBranchState(commit, commitBranchName)
        if(isMergeOnActiveBranch(commit)) {
            processMerge(commit, commitBranchName)
        }
    }

    private fun updateBranchState(commit: Commit, commitBranchName: NamedBranch) {
        val namedBranch = namedBranches.getOrPut(commitBranchName.name) {
            MutableBranch(commit.hash, commit.date, commit.hash, commit.date, commitBranchName)
        }
        if (commit.date < namedBranch.firstCommitDate) {
            namedBranch.firstCommit(commit.hash, commit.date)
        }
        if (commit.date > namedBranch.lastCommitDate) {
            namedBranch.lastCommit(commit.hash, commit.date)
        }
    }

    private fun processMerge(commit: Commit, commitBranchName: NamedBranch) {
        val mergedParentHash = commit.parents[1]
        when (val mergedParentBranchName = branchByHash[mergedParentHash]) {
            is NamedBranch -> recordNamedMerge(
                commit, commitBranchName.name,
                mergedParentHash, mergedParentBranchName
            )
            else -> recordUnnamedMerge(commit, commitBranchName.name, mergedParentHash)
        }
    }

    private fun isMergeOnActiveBranch(commit: Commit): Boolean = (!hasActiveBranch || !commit.isNotOnActiveBranch) && !commit.isNotMergeCommit

    private fun recordNamedMerge(
        mergeCommit: Commit,
        mergeCommitBranchName: BranchName,
        mergedParentHash: CommitHash,
        branchName: NamedBranch
    ) {
        val mergedCommitDate = commitByHash[mergedParentHash]?.date ?: mergeCommit.date
        val mergedState = namedBranches.getOrPut(branchName.name) {
            MutableBranch(mergedParentHash, mergedCommitDate, mergedParentHash, mergedCommitDate, branchName)
        }
        if (mergedState.mergeCommitHash == null) {
            mergedState.mergeCommit(mergeCommit.hash, mergeCommit.date, mergeCommitBranchName)
        }
    }

    private fun recordUnnamedMerge(mergeCommit: Commit, targetBranchName: BranchName, mergedParentHash: CommitHash) {
        val firstCommitOfUnnamed = findFirstCommitOfBranch(commits, mergedParentHash, branchByHash)
        val lastCommitOfUnnamed = findLastCommitOfBranch(commits, mergedParentHash)
        val unnamedState = unnamedBranches.getOrPut(firstCommitOfUnnamed.hash) {
            MutableBranch(
                firstCommitOfUnnamed.hash,
                firstCommitOfUnnamed.date,
                lastCommitOfUnnamed.hash,
                lastCommitOfUnnamed.date,
                NamelessBranch
            )
        }
        if (unnamedState.mergeCommitHash == null) {
            unnamedState.mergeCommit(mergeCommit.hash, mergeCommit.date, targetBranchName)
        }
    }

    fun toBranches(): Branches {
        val namedResult = namedBranches.values.map(MutableBranch::toBranch)
        val unnamedResult = unnamedBranches.values.map(MutableBranch::toBranch)
        return Branches(namedResult + unnamedResult)
    }
}

private fun findFirstCommitOfBranch(
    commits: List<Commit>,
    startHash: CommitHash,
    branchByHash: Map<CommitHash, BranchNameCertainty?>
): Commit {
    val commitByHash = commits.associateBy { it.hash }
    var current = commitByHash[startHash] ?: return commits.first { it.hash == startHash }

    while (true) {
        val parent = current.parents.firstOrNull() ?: break
        val parentCommit = commitByHash[parent] ?: break
        val parentBranch = branchByHash[parent]
        if (parentBranch is NamedBranch.Certain || parentBranch is NamedBranch.Inferred) {
            break
        }
        current = parentCommit
    }

    return current
}

private fun findLastCommitOfBranch(commits: List<Commit>, startHash: CommitHash): Commit {
    val commitByHash = commits.associateBy { it.hash }
    return commitByHash[startHash] ?: commits.first { it.hash == startHash }
}

private class MutableBranch(
    firstCommitHash: CommitHash,
    firstCommitDate: ZonedDateTime,
    lastCommitHash: CommitHash,
    lastCommitDate: ZonedDateTime,
    val branchNameCertainty: BranchNameCertainty
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

    fun firstCommit(hash: CommitHash, date: ZonedDateTime) {
        firstCommitHash = hash
        firstCommitDate = date
    }

    fun lastCommit(hash: CommitHash, date: ZonedDateTime) {
        lastCommitHash = hash
        lastCommitDate = date
    }

    fun mergeCommit(hash: CommitHash, date: ZonedDateTime, target: BranchName?) {
        mergeCommitHash = hash
        mergeCommitDate = date
        targetBranch = target
    }

    fun toBranch(): Branch = Branch(
        branchNameCertainty = branchNameCertainty,
        firstCommitHash = firstCommitHash,
        firstCommitDate = firstCommitDate,
        lastCommitHash = lastCommitHash,
        lastCommitDate = lastCommitDate,
        mergeCommitHash = mergeCommitHash,
        mergeDate = mergeCommitDate,
        targetBranch = targetBranch
    )
}
