package de.richargh.teamcharta.importer.gitmining.app.internal

import de.richargh.teamcharta.importer.git.app.api.BranchNameCertainty
import de.richargh.teamcharta.importer.git.app.api.BranchName
import de.richargh.teamcharta.importer.git.app.api.Named
import de.richargh.teamcharta.importer.git.app.api.Nameless
import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.git.app.api.CommitHash
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

    private val namedBranchStates = mutableMapOf<BranchName, MutableBranch>()
    private val unnamedBranchStates = mutableMapOf<CommitHash, MutableBranch>()

    fun processCommits() {
        commits.forEach(::processCommit)
    }

    private fun processCommit(commit: Commit) {
        val branchNameCertainty: Named = when(val certainty = commit.branch){
            is Nameless -> return
            is Named -> certainty
        }

        updateBranchState(commit, branchNameCertainty)
        processMergeIfApplicable(commit, branchNameCertainty)
    }

    private fun updateBranchState(commit: Commit, commitBranch: Named) {
        val state = namedBranchStates.getOrPut(commitBranch.name) {
            MutableBranch(commit.hash, commit.date, commit.hash, commit.date, commitBranch)
        }
        if (commit.date < state.firstCommitDate) {
            state.firstCommit(commit.hash, commit.date)
        }
        if (commit.date > state.lastCommitDate) {
            state.lastCommit(commit.hash, commit.date)
        }
    }

    private fun processMergeIfApplicable(commit: Commit, commitBranch: Named) {
        val shouldRecordMerge = !hasActiveBranch || commit.isOnActiveBranch
        if (!commit.isMergeCommit || !shouldRecordMerge) return

        val mergedParentHash = commit.parents[1]
        when (val mergedBranch = branchByHash[mergedParentHash]) {
            is Named.Certain -> recordNamedMerge(commit, commitBranch.name, mergedParentHash, mergedBranch.name, Named.Certain(mergedBranch.name))
            is Named.Inferred -> recordNamedMerge(commit, commitBranch.name, mergedParentHash, mergedBranch.name, Named.Inferred(mergedBranch.name))
            else -> recordUnnamedMerge(commit, commitBranch.name, mergedParentHash)
        }
    }

    private fun recordNamedMerge(
        mergeCommit: Commit,
        targetBranchName: BranchName,
        mergedParentHash: CommitHash,
        mergedBranchName: BranchName,
        branchNameCertainty: BranchNameCertainty
    ) {
        val mergedCommitDate = commitByHash[mergedParentHash]?.date ?: mergeCommit.date
        val mergedState = namedBranchStates.getOrPut(mergedBranchName) {
            MutableBranch(mergedParentHash, mergedCommitDate, mergedParentHash, mergedCommitDate, branchNameCertainty)
        }
        if (mergedState.mergeCommitHash == null) {
            mergedState.mergeCommit(mergeCommit.hash, mergeCommit.date, targetBranchName)
        }
    }

    private fun recordUnnamedMerge(mergeCommit: Commit, targetBranchName: BranchName, mergedParentHash: CommitHash) {
        val firstCommitOfUnnamed = findFirstCommitOfBranch(commits, mergedParentHash, branchByHash)
        val lastCommitOfUnnamed = findLastCommitOfBranch(commits, mergedParentHash)
        val unnamedState = unnamedBranchStates.getOrPut(firstCommitOfUnnamed.hash) {
            MutableBranch(
                firstCommitOfUnnamed.hash,
                firstCommitOfUnnamed.date,
                lastCommitOfUnnamed.hash,
                lastCommitOfUnnamed.date,
                Nameless
            )
        }
        if (unnamedState.mergeCommitHash == null) {
            unnamedState.mergeCommit(mergeCommit.hash, mergeCommit.date, targetBranchName)
        }
    }

    fun toBranches(): Branches {
        val namedResult = namedBranchStates.values.map(MutableBranch::toBranch)
        val unnamedResult = unnamedBranchStates.values.map(MutableBranch::toBranch)
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
        if (parentBranch is Named.Certain || parentBranch is Named.Inferred) {
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
