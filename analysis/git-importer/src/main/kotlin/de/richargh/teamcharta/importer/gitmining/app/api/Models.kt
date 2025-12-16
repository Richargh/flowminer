package de.richargh.teamcharta.importer.gitmining.app.api

import de.richargh.teamcharta.importer.git.app.api.BranchName
import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import de.richargh.teamcharta.importer.git.app.api.NameCertainty
import java.time.ZonedDateTime

data class Branch(
    val nameCertainty: NameCertainty,
    val firstCommitHash: CommitHash,
    val firstCommitDate: ZonedDateTime,
    val lastCommitHash: CommitHash,
    val lastCommitDate: ZonedDateTime,
    val mergeCommitHash: CommitHash?,
    val mergeDate: ZonedDateTime?,
    val targetBranch: BranchName?
) {
    val name: BranchName? get() = when (nameCertainty) {
        is NameCertainty.Named.Certain -> nameCertainty.name
        is NameCertainty.Named.Inferred -> nameCertainty.name
        is NameCertainty.Nameless -> null
    }

    val isNamed: Boolean get() = nameCertainty is NameCertainty.Named
    val isUnnamed: Boolean get() = nameCertainty is NameCertainty.Nameless
    val isInferred: Boolean get() = nameCertainty is NameCertainty.Named.Inferred
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