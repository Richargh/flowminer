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

private class BranchCollector(private val allCommits: List<Commit>) {
    private val branchByHash = mutableMapOf<CommitHash, BranchNameCertainty>()
    private val commitByHash = mutableMapOf<CommitHash, Commit>()
    private var hasNoActiveBranch = true

    private val namedBranches = mutableMapOf<BranchName, MutableBranch>()
    private val unnamedBranches = mutableMapOf<CommitHash, MutableBranch>()

    fun processCommits() {
        val pendingMerges = mutableListOf<PendingMerge>()

        for (commit in allCommits) {
            if (commit.isOnActiveBranch) hasNoActiveBranch = false

            branchByHash[commit.hash] = commit.branch
            commitByHash[commit.hash] = commit

            val branchName = commit.branch as? NamedBranch ?: continue
            updateBranch(commit, branchName)
            if (commit.isMerge) {
                pendingMerges.add(PendingMerge(commit, branchName))
            }
        }

        for (pending in pendingMerges) {
            if (hasNoActiveBranch || pending.commit.isOnActiveBranch) {
                processFeatureBranches(pending.commit, pending.branchName)
            }
        }
    }

    private fun updateBranch(commit: Commit, commitBranchName: NamedBranch) {
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

    private fun processFeatureBranches(commit: Commit, commitBranchName: NamedBranch) {
        val featureBranchHash = commit.parents[1]
        when (val featureBranchName = branchByHash[featureBranchHash]) {
            is NamedBranch -> recordNamedFeatureBranch(
                commit,
                commitBranchName.name,
                featureBranchHash,
                featureBranchName
            )

            is NamelessBranch, null -> recordUnnamedFeatureBranch(commit, commitBranchName.name, featureBranchHash)
        }
    }

    private fun recordNamedFeatureBranch(
        commit: Commit,
        commitBranchName: BranchName,
        featureBranchHash: CommitHash,
        featureBranchName: NamedBranch
    ) {
        val mergedCommitDate = commitByHash[featureBranchHash]?.date ?: commit.date
        val featureBranch = namedBranches.getOrPut(featureBranchName.name) {
            MutableBranch(featureBranchHash, mergedCommitDate, featureBranchHash, mergedCommitDate, featureBranchName)
        }
        if (featureBranch.mergeCommitHash == null) {
            featureBranch.mergeCommit(commit.hash, commit.date, commitBranchName)
        }
    }

    private fun recordUnnamedFeatureBranch(commit: Commit, commitBranchName: BranchName, featureBranchHash: CommitHash) {
        val firstCommitOfUnnamed = findFirstCommitOfBranch(allCommits, featureBranchHash, branchByHash)
        val lastCommitOfUnnamed = findLastCommitOfBranch(allCommits, featureBranchHash)
        val unnamedBranch = unnamedBranches.getOrPut(firstCommitOfUnnamed.hash) {
            MutableBranch(
                firstCommitOfUnnamed.hash,
                firstCommitOfUnnamed.date,
                lastCommitOfUnnamed.hash,
                lastCommitOfUnnamed.date,
                NamelessBranch
            )
        }
        if (unnamedBranch.mergeCommitHash == null) {
            unnamedBranch.mergeCommit(commit.hash, commit.date, commitBranchName)
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

private data class PendingMerge(val commit: Commit, val branchName: NamedBranch)

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
