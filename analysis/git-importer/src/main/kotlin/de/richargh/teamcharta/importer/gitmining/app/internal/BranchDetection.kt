package de.richargh.teamcharta.importer.gitmining.app.internal

import de.richargh.teamcharta.importer.git.app.api.*
import de.richargh.teamcharta.importer.gitmining.app.api.Branch
import de.richargh.teamcharta.importer.gitmining.app.api.Branches
import de.richargh.teamcharta.importer.gitmining.app.api.NameCertainty
import java.time.ZonedDateTime

fun extractBranchInfo(commits: List<Commit>): Branches {
    val branchByHash = commits.associate { it.hash to it.branch }
    val commitByHash = commits.associateBy { it.hash }
    val hasActiveBranch = commits.any { it.isOnActiveBranch }

    val namedBranchStates = mutableMapOf<BranchName, MutableBranch>()
    val unnamedBranchStates = mutableMapOf<CommitHash, MutableBranch>()

    for (commit in commits) {
        val branch = commit.branch
        val (branchName, nameCertainty) = when (branch) {
            is BranchAssignment.Certain -> branch.name to NameCertainty.Certain(branch.name)
            is BranchAssignment.Inferred -> branch.name to NameCertainty.Inferred(branch.name)
            else -> continue
        }

        val state = namedBranchStates.getOrPut(branchName) {
            MutableBranch(commit.hash, commit.date, commit.hash, commit.date, nameCertainty = nameCertainty)
        }
        if (commit.date < state.firstCommitDate) {
            state.firstCommitHash = commit.hash
            state.firstCommitDate = commit.date
        }
        if (commit.date > state.lastCommitDate) {
            state.lastCommitHash = commit.hash
            state.lastCommitDate = commit.date
        }


        val shouldRecordMerge = !hasActiveBranch || commit.isOnActiveBranch
        if (commit.isMergeCommit && shouldRecordMerge) {
            val mergedParentHash = commit.parents[1]
            val mergedBranch = branchByHash[mergedParentHash]
            when (mergedBranch) {
                is BranchAssignment.Certain -> {
                    val mergedCommit = commitByHash[mergedParentHash]
                    val mergedCommitDate = mergedCommit?.date ?: commit.date
                    val mergedState = namedBranchStates.getOrPut(mergedBranch.name) {
                        MutableBranch(mergedParentHash, mergedCommitDate, mergedParentHash, mergedCommitDate, nameCertainty = NameCertainty.Certain(mergedBranch.name))
                    }
                    if (mergedState.mergeCommitHash == null) {
                        mergedState.mergeCommitHash = commit.hash
                        mergedState.mergeDate = commit.date
                        mergedState.targetBranch = branchName
                    }
                }
                is BranchAssignment.Inferred -> {
                    val mergedCommit = commitByHash[mergedParentHash]
                    val mergedCommitDate = mergedCommit?.date ?: commit.date
                    val mergedState = namedBranchStates.getOrPut(mergedBranch.name) {
                        MutableBranch(mergedParentHash, mergedCommitDate, mergedParentHash, mergedCommitDate, nameCertainty = NameCertainty.Inferred(mergedBranch.name))
                    }
                    if (mergedState.mergeCommitHash == null) {
                        mergedState.mergeCommitHash = commit.hash
                        mergedState.mergeDate = commit.date
                        mergedState.targetBranch = branchName
                    }
                }
                else -> {
                    // Unknown or null branch - track as unnamed
                    val firstCommitOfUnnamed = findFirstCommitOfBranch(commits, mergedParentHash, branchByHash)
                    val lastCommitOfUnnamed = findLastCommitOfBranch(commits, mergedParentHash, branchByHash)
                    val unnamedState = unnamedBranchStates.getOrPut(firstCommitOfUnnamed.hash) {
                        MutableBranch(firstCommitOfUnnamed.hash, firstCommitOfUnnamed.date, lastCommitOfUnnamed.hash, lastCommitOfUnnamed.date, nameCertainty = NameCertainty.Nameless)
                    }
                    if (unnamedState.mergeCommitHash == null) {
                        unnamedState.mergeCommitHash = commit.hash
                        unnamedState.mergeDate = commit.date
                        unnamedState.targetBranch = branchName
                    }
                }
            }
        }
    }

    val namedResult = namedBranchStates.map { (_, state) ->
        Branch(
            firstCommitHash = state.firstCommitHash,
            firstCommitDate = state.firstCommitDate,
            lastCommitHash = state.lastCommitHash,
            lastCommitDate = state.lastCommitDate,
            mergeCommitHash = state.mergeCommitHash,
            mergeDate = state.mergeDate,
            targetBranch = state.targetBranch,
            nameCertainty = state.nameCertainty
        )
    }

    val unnamedResult = unnamedBranchStates.map { (_, state) ->
        Branch(
            firstCommitHash = state.firstCommitHash,
            firstCommitDate = state.firstCommitDate,
            lastCommitHash = state.lastCommitHash,
            lastCommitDate = state.lastCommitDate,
            mergeCommitHash = state.mergeCommitHash,
            mergeDate = state.mergeDate,
            targetBranch = state.targetBranch,
            nameCertainty = NameCertainty.Nameless
        )
    }

    return Branches(namedResult + unnamedResult)
}

private fun findFirstCommitOfBranch(
    commits: List<Commit>,
    startHash: CommitHash,
    branchByHash: Map<CommitHash, BranchAssignment?>
): Commit {
    val commitByHash = commits.associateBy { it.hash }
    var current = commitByHash[startHash] ?: return commits.first { it.hash == startHash }

    while (true) {
        val parent = current.parents.firstOrNull() ?: break
        val parentCommit = commitByHash[parent] ?: break
        val parentBranch = branchByHash[parent]
        // Stop if parent belongs to a different (named) branch
        if (parentBranch is BranchAssignment.Certain || parentBranch is BranchAssignment.Inferred) {
            break
        }
        current = parentCommit
    }

    return current
}

private fun findLastCommitOfBranch(
    commits: List<Commit>,
    startHash: CommitHash,
    @Suppress("UNUSED_PARAMETER") branchByHash: Map<CommitHash, BranchAssignment?>
): Commit {
    // startHash is typically the second parent of a merge commit (the tip of the merged branch)
    // This is already the last commit on that branch
    val commitByHash = commits.associateBy { it.hash }
    return commitByHash[startHash] ?: commits.first { it.hash == startHash }
}

private data class MutableBranch(
    var firstCommitHash: CommitHash,
    var firstCommitDate: ZonedDateTime,
    var lastCommitHash: CommitHash,
    var lastCommitDate: ZonedDateTime,
    var mergeCommitHash: CommitHash? = null,
    var mergeDate: ZonedDateTime? = null,
    var targetBranch: BranchName? = null,
    var nameCertainty: NameCertainty
)