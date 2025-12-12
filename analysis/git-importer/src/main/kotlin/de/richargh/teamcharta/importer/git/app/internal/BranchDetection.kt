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

fun extractBranchInfo(commits: List<Commit>): BranchInfos {
    // Find all branches referenced in commits, keyed by branch name
    val branchCommits = mutableMapOf<String, MutableList<Commit>>()

    for (commit in commits) {
        for (ref in commit.refs) {
            val branchRef = extractBranchRef(ref) ?: continue
            branchCommits.getOrPut(branchRef.name) { mutableListOf() }.add(commit)
        }
    }

    // Build commit hash -> branch name lookup
    val commitToBranch = mutableMapOf<String, String>()
    for ((branchName, branchCommitsList) in branchCommits) {
        for (commit in branchCommitsList) {
            commitToBranch[commit.hash.rawValue] = branchName
        }
    }

    // Find merge commits and associate them with branches
    val mergeInfo = mutableMapOf<String, Pair<Commit, Ref.Branch>>() // branchName -> (mergeCommit, targetBranch)

    for (commit in commits) {
        if (commit.parents.size > 1) { // Merge commit
            val secondParentHash = commit.parents.getOrNull(1)?.rawValue ?: continue
            val mergedBranchName = commitToBranch[secondParentHash] ?: continue
            val targetBranch = commit.refs.firstNotNullOfOrNull { extractBranchRef(it) }
            if (targetBranch != null) {
                mergeInfo[mergedBranchName] = commit to targetBranch
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

    return BranchInfos(result)
}
