package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api2.BranchInfo
import de.richargh.teamcharta.importer.git.app.api2.BranchInfos
import de.richargh.teamcharta.importer.git.app.api2.Commit
import de.richargh.teamcharta.importer.git.app.api2.Ref

fun extractBranchRef(ref: Ref): Ref.Branch? {
    return when (ref) {
        is Ref.Head -> Ref.Branch(ref.branchName)
        is Ref.Branch -> {
            val name = if (ref.name.startsWith("origin/")) {
                ref.name.removePrefix("origin/")
            } else {
                ref.name
            }
            Ref.Branch(name)
        }
        is Ref.Tag -> null
    }
}

private val mergePattern = Regex("Merge branch '([^']+)'")

fun extractMergedBranchFromMessage(message: String): Ref.Branch? {
    val match = mergePattern.find(message) ?: return null
    return match.groupValues.getOrNull(1)?.let { Ref.Branch(it) }
}

fun extractBranchInfo(commits: List<Commit>): BranchInfos {
    // Find all branches referenced in commits, keyed by full ref name
    val branchCommits = mutableMapOf<String, MutableList<Commit>>()

    for (commit in commits) {
        for (ref in commit.refs) {
            val branchRef = extractBranchRef(ref) ?: continue
            branchCommits.getOrPut(branchRef.name) { mutableListOf() }.add(commit)
        }
    }

    // Find merge commits and associate them with branches
    val mergeInfo = mutableMapOf<String, Pair<Commit, Ref.Branch>>() // branchName -> (mergeCommit, targetBranch)

    for (commit in commits) {
        if (commit.parents.size > 1) { // Merge commit
            val mergedBranch = extractMergedBranchFromMessage(commit.message) ?: continue
            val targetBranch = commit.refs.firstNotNullOfOrNull { extractBranchRef(it) }
            if (targetBranch != null) {
                mergeInfo[mergedBranch.name] = commit to targetBranch
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
            name = Ref.Branch(branchName),
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
                name = Ref.Branch(branchName),
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
