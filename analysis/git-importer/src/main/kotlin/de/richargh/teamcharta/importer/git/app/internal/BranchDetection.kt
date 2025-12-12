package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api2.BranchInfo
import de.richargh.teamcharta.importer.git.app.api2.BranchInfos
import de.richargh.teamcharta.importer.git.app.api2.Commit
import de.richargh.teamcharta.importer.git.app.api2.Ref


fun extractBranchInfo(commits: List<Commit>): BranchInfos {
    // Build commit hash -> Commit lookup
    val commitByHash = commits.associateBy { it.hash.rawValue }

    // Find all tip commits (commits with branch refs)
    val branchTips = mutableMapOf<String, Commit>()
    for (commit in commits) {
        for (ref in commit.refs) {
            val branchRef = extractBranchRef(ref) ?: continue
            branchTips[branchRef.name] = commit
        }
    }

    // For each branch, walk backwards through parents to find all commits on that branch
    // Stop when we hit a commit already claimed by another branch
    // Process main/master branches first so they claim their commits before feature branches
    val branchCommits = mutableMapOf<String, MutableList<Commit>>()
    val commitToBranch = mutableMapOf<String, String>()

    val sortedBranches = branchTips.entries.sortedBy { (name, _) ->
        when {
            name.contains("main") || name.contains("master") -> 0
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

            // For merge commits, follow second parent (index 1) to stay on same branch
            // For regular commits, follow first parent (index 0)
            val nextParentHash = if (current.parents.size > 1) {
                current.parents.getOrNull(1)?.rawValue
            } else {
                current.parents.firstOrNull()?.rawValue
            }

            current = nextParentHash?.let { commitByHash[it] }
        }

        branchCommits[branchName] = commitsOnBranch
    }

    // Find merge commits and associate them with branches
    val mergeInfo = mutableMapOf<String, Pair<Commit, Ref.Branch>>() // branchName -> (mergeCommit, targetBranch)

    for (commit in commits) {
        if (commit.parents.size > 1) { // Merge commit
            // First parent (index 0) is the branch being merged
            val firstParentHash = commit.parents.firstOrNull()?.rawValue ?: continue
            val mergedBranchName = commitToBranch[firstParentHash] ?: continue
            val targetBranch = commit.refs.firstNotNullOfOrNull { extractBranchRef(it) }
            if (targetBranch != null && mergedBranchName != targetBranch.name) {
                mergeInfo[mergedBranchName] = commit to targetBranch
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

private fun extractBranchRef(ref: Ref): Ref.Branch? {
    return when (ref) {
        is Ref.Head -> Ref.Branch(ref.branchName)
        is Ref.Branch -> Ref.Branch(ref.name)
        is Ref.Tag -> null
    }
}