package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api2.BranchInfo
import de.richargh.teamcharta.importer.git.app.api2.BranchInfos
import de.richargh.teamcharta.importer.git.app.api2.Commit
import kotlin.collections.iterator

fun extractBranchName(ref: String): String? {
    return when {
        ref.startsWith("HEAD -> ") -> ref.removePrefix("HEAD -> ")
        ref.startsWith("origin/") -> ref.removePrefix("origin/")
        ref.contains("tag: ") -> null
        else -> ref
    }
}

private val mergePattern = Regex("Merge branch '([^']+)'")

fun extractMergedBranchFromMessage(message: String): String? {
    val match = mergePattern.find(message) ?: return null
    return match.groupValues.getOrNull(1)
}

fun extractBranchInfo(commits: List<Commit>): BranchInfos {
    // Find all branches referenced in commits
    val branchCommits = mutableMapOf<String, MutableList<Commit>>()

    for (commit in commits) {
        for (ref in commit.refs) {
            val branchName = extractBranchName(ref) ?: continue
            branchCommits.getOrPut(branchName) { mutableListOf() }.add(commit)
        }
    }

    // Find merge commits and associate them with branches
    val mergeInfo = mutableMapOf<String, Pair<Commit, String>>() // branchName -> (mergeCommit, targetBranch)

    for (commit in commits) {
        if (commit.parents.size > 1) { // Merge commit
            val mergedBranch = extractMergedBranchFromMessage(commit.message) ?: continue
            val targetBranch = commit.refs.firstNotNullOfOrNull { extractBranchName(it) }
            if (targetBranch != null) {
                mergeInfo[mergedBranch] = commit to targetBranch
            }
        }
    }

    // Build BranchInfo for each branch
    val result = mutableListOf<BranchInfo>()

    for ((branchName, branchCommitsList) in branchCommits) {
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

    // Also add branches that are only known from merge commits
    for ((branchName, mergeData) in mergeInfo) {
        if (!branchCommits.containsKey(branchName)) {
            // We don't have refs pointing to this branch, but we know it was merged
            // Need to find the first commit for this branch - for now, use merge info
            result.add(BranchInfo.merged(
                name = branchName,
                firstCommitHash = mergeData.first.parents.getOrNull(1) ?: mergeData.first.hash,
                firstCommitDate = mergeData.first.date, // Approximation - we don't know the actual first commit date
                mergeCommitHash = mergeData.first.hash,
                mergeDate = mergeData.first.date,
                targetBranch = mergeData.second
            ))
        }
    }

    return BranchInfos(result)
}
