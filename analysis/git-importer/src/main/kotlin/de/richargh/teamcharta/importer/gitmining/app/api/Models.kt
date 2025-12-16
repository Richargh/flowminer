package de.richargh.teamcharta.importer.gitmining.app.api

import de.richargh.teamcharta.importer.git.app.api.BranchName
import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import de.richargh.teamcharta.importer.git.app.api.BranchNameCertainty
import java.time.ZonedDateTime

data class Branch(
    val branchNameCertainty: BranchNameCertainty,
    val firstCommitHash: CommitHash,
    val firstCommitDate: ZonedDateTime,
    val lastCommitHash: CommitHash,
    val lastCommitDate: ZonedDateTime,
    val mergeCommitHash: CommitHash?,
    val mergeDate: ZonedDateTime?,
    val targetBranch: BranchName?
) {
    val name: BranchName? get() = when (branchNameCertainty) {
        is BranchNameCertainty.Named.Certain -> branchNameCertainty.name
        is BranchNameCertainty.Named.Inferred -> branchNameCertainty.name
        is BranchNameCertainty.Nameless -> null
    }

    val isNamed: Boolean get() = branchNameCertainty is BranchNameCertainty.Named
    val isUnnamed: Boolean get() = branchNameCertainty is BranchNameCertainty.Nameless
    val isInferred: Boolean get() = branchNameCertainty is BranchNameCertainty.Named.Inferred
}

class Commits(private val commits: List<Commit>) {
    operator fun get(index: Int): Commit = commits[index]

    fun all(): Collection<Commit> = commits
    fun first(): Commit = commits.first()
    fun size(): Int = commits.size
    fun isEmpty(): Boolean = commits.isEmpty()
    fun isNotEmpty(): Boolean = commits.isNotEmpty()
}

class Branches(branches: List<Branch>) {
    private val namedBranches: Map<BranchName, Branch> = branches
        .filter { it.isNamed }
        .associateBy { it.name!! }

    val unnamed: List<Branch> = branches.filter { it.isUnnamed }

    operator fun get(name: BranchName): Branch? = namedBranches[name]
    operator fun get(name: String): Branch? = namedBranches[BranchName(name)]

    fun all(): Collection<Branch> = namedBranches.values + unnamed
    fun size() = namedBranches.size + unnamed.size
}

data class GitMiningResult(
    val commits: Commits,
    val branches: Branches = Branches(emptyList())
)