package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api2.BranchInfo
import de.richargh.teamcharta.importer.git.app.api2.BranchInfos
import de.richargh.teamcharta.importer.git.app.api2.BranchName
import de.richargh.teamcharta.importer.git.app.api2.Commit
import de.richargh.teamcharta.importer.git.app.api2.Ref


fun extractBranchInfo(commits: List<Commit>): BranchInfos {
    // Build commit hash -> Commit lookup
    val commitByHash = commits.associateBy { it.hash.rawValue }

    // Find all tip commits (commits with remote branch refs)
    // Only consider BranchTip refs, not Head refs (we track remote branches, not local ones)
    val branchTips = mutableMapOf<BranchName, Commit>()
    for (commit in commits) {
        for (ref in commit.refs.filterIsInstance<Ref.BranchTip>()) {
            branchTips[ref.name] = commit
        }
    }

    // For each branch, walk backwards through parents to find all commits on that branch
    // Stop when we hit a commit already claimed by another branch
    // Process main/master branches first so they claim their commits before feature branches
    val branchCommits = mutableMapOf<BranchName, MutableList<Commit>>()
    val commitToBranch = mutableMapOf<String, BranchName>()

    val sortedBranches = branchTips.entries.sortedBy { (name, _) ->
        when {
            name.value.contains("main") || name.value.contains("master") || name.value.contains("trunk") -> 0
            else -> 1
        }
    }

    for ((branchName, tipCommit) in sortedBranches) {
        val commitsOnBranch = mutableListOf<Commit>()
        var current: Commit? = tipCommit

        while (current != null) {
            val hash = current.hash.rawValue

            // Stop if this commit is already assigned to another branch
            if (hash in commitToBranch && commitToBranch[hash] != branchName) {
                break
            }

            // Skip if already visited for this branch
            if (hash in commitToBranch && commitToBranch[hash] == branchName) {
                break
            }

            commitsOnBranch.add(current)
            commitToBranch[hash] = branchName

            // Always follow first parent to stay on the same branch
            // In git, first parent is the branch you were on when merging
            val nextParentHash = current.parents.firstOrNull()?.rawValue

            current = nextParentHash?.let { commitByHash[it] }
        }

        branchCommits[branchName] = commitsOnBranch
    }

    // Find merge commits and associate them with branches
    // Only record merges INTO main/master/trunk branches (feature branches are "complete" when merged into main)
    val mergeInfo = mutableMapOf<BranchName, Pair<Commit, BranchName>>() // branchName -> (mergeCommit, targetBranch)

    for (commit in commits) {
        if (commit.parents.size > 1) { // Merge commit
            // Second parent (index 1) is the branch being merged (git convention)
            val secondParentHash = commit.parents.getOrNull(1)?.rawValue ?: continue
            val mergedBranchName = commitToBranch[secondParentHash] ?: continue
            val targetBranch = commit.refs.filterIsInstance<Ref.BranchTip>().firstOrNull()?.name
                ?: commit.refs.filterIsInstance<Ref.LocalHead>().firstOrNull()?.branch
            if (targetBranch != null && mergedBranchName != targetBranch) {
                // Only record if target is a main branch (merging into main = branch complete)
                val isTargetMainBranch = targetBranch.value.contains("main") ||
                        targetBranch.value.contains("master") ||
                        targetBranch.value.contains("trunk")
                if (isTargetMainBranch) {
                    mergeInfo[mergedBranchName] = commit to targetBranch
                }
            }
        }
    }

    // Build BranchInfo for each branch
    val result = mutableListOf<BranchInfo>()

    for ((branchName, branchCommitsList) in branchCommits) {
        // Skip branches with no commits (e.g., when multiple refs point to the same commit)
        if (branchCommitsList.isEmpty()) continue

        val sortedCommits = branchCommitsList.sortedBy { it.date }
        val firstCommit = sortedCommits.first()
        val merge = mergeInfo[branchName]

        result.add(BranchInfo(
            name = branchName,
            firstCommitHash = firstCommit.hash,
            firstCommitDate = firstCommit.date,
            mergeCommitHash = merge?.first?.hash,
            mergeDate = merge?.first?.date,
            targetBranch = merge?.second
        ))
    }

    return BranchInfos(result)
}