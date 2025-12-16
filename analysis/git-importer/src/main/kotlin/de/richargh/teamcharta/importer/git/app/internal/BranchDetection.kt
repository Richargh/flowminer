package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api.*
import java.time.ZonedDateTime

fun extractBranchInfo(commits: List<Commit>): BranchInfos {
    val branchByHash = commits.associate { it.hash to it.branch }
    val hasActiveBranch = commits.any { it.isOnActiveBranch }

    data class BranchState(
        var firstCommitHash: CommitHash,
        var firstCommitDate: ZonedDateTime,
        var mergeCommitHash: CommitHash? = null,
        var mergeDate: ZonedDateTime? = null,
        var targetBranch: BranchName? = null,
        var nameCertainty: NameCertainty
    )

    val namedBranchStates = mutableMapOf<BranchName, BranchState>()
    val unnamedBranchStates = mutableMapOf<CommitHash, BranchState>()

    for (commit in commits) {
        val branch = commit.branch
        val (branchName, nameCertainty) = when (branch) {
            is BranchAssignment.Certain -> branch.name to NameCertainty.Certain(branch.name)
            is BranchAssignment.Inferred -> branch.name to NameCertainty.Inferred(branch.name)
            else -> continue
        }

        val state = namedBranchStates.getOrPut(branchName) {
            BranchState(commit.hash, commit.date, nameCertainty = nameCertainty)
        }
        if (commit.date < state.firstCommitDate) {
            state.firstCommitHash = commit.hash
            state.firstCommitDate = commit.date
        }


        val shouldRecordMerge = !hasActiveBranch || commit.isOnActiveBranch
        if (commit.isMergeCommit && shouldRecordMerge) {
            val mergedParentHash = commit.parents[1]
            val mergedBranch = branchByHash[mergedParentHash]
            when (mergedBranch) {
                is BranchAssignment.Certain -> {
                    val mergedState = namedBranchStates.getOrPut(mergedBranch.name) {
                        BranchState(mergedParentHash, commit.date, nameCertainty = NameCertainty.Certain(mergedBranch.name))
                    }
                    if (mergedState.mergeCommitHash == null) {
                        mergedState.mergeCommitHash = commit.hash
                        mergedState.mergeDate = commit.date
                        mergedState.targetBranch = branchName
                    }
                }
                is BranchAssignment.Inferred -> {
                    val mergedState = namedBranchStates.getOrPut(mergedBranch.name) {
                        BranchState(mergedParentHash, commit.date, nameCertainty = NameCertainty.Inferred(mergedBranch.name))
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
                    val unnamedState = unnamedBranchStates.getOrPut(firstCommitOfUnnamed.hash) {
                        BranchState(firstCommitOfUnnamed.hash, firstCommitOfUnnamed.date, nameCertainty = NameCertainty.Nameless)
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
        BranchInfo(
            nameCertainty = state.nameCertainty,
            firstCommitHash = state.firstCommitHash,
            firstCommitDate = state.firstCommitDate,
            mergeCommitHash = state.mergeCommitHash,
            mergeDate = state.mergeDate,
            targetBranch = state.targetBranch
        )
    }

    val unnamedResult = unnamedBranchStates.map { (_, state) ->
        BranchInfo(
            nameCertainty = NameCertainty.Nameless,
            firstCommitHash = state.firstCommitHash,
            firstCommitDate = state.firstCommitDate,
            mergeCommitHash = state.mergeCommitHash,
            mergeDate = state.mergeDate,
            targetBranch = state.targetBranch
        )
    }

    return BranchInfos(namedResult + unnamedResult)
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