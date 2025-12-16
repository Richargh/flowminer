package de.richargh.teamcharta.importer.gitmining.app.api

import de.richargh.teamcharta.importer.git.app.api.BranchName
import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import java.time.ZonedDateTime

/** Certainty level of a branch's name in BranchInfo */
sealed interface NameCertainty {
    /** Branch ref exists - name is certain */
    data class Certain(val name: BranchName) : NameCertainty

    /** Inferred from merge commit message */
    data class Inferred(val name: BranchName) : NameCertainty

    /** Cannot determine name - branch is unnamed */
    object Nameless : NameCertainty
}

data class BranchInfo(
    val firstCommitHash: CommitHash,
    val firstCommitDate: ZonedDateTime,
    val lastCommitHash: CommitHash,
    val lastCommitDate: ZonedDateTime,
    val mergeCommitHash: CommitHash?,
    val mergeDate: ZonedDateTime?,
    val targetBranch: BranchName?,
    val nameCertainty: NameCertainty
) {
    /** Returns the branch name, or null if unnamed */
    val name: BranchName? get() = when (nameCertainty) {
        is NameCertainty.Certain -> nameCertainty.name
        is NameCertainty.Inferred -> nameCertainty.name
        is NameCertainty.Nameless -> null
    }

    val isNamed: Boolean get() = nameCertainty !is NameCertainty.Nameless
    val isUnnamed: Boolean get() = nameCertainty is NameCertainty.Nameless
    val isInferred: Boolean get() = nameCertainty is NameCertainty.Inferred
}

class Commits(private val commits: List<Commit>) {
    operator fun get(index: Int): Commit = commits[index]

    fun all(): Collection<Commit> = commits
    fun first(): Commit = commits.first()
    fun size(): Int = commits.size
    fun isEmpty(): Boolean = commits.isEmpty()
    fun isNotEmpty(): Boolean = commits.isNotEmpty()
}

class BranchInfos(branches: List<BranchInfo>) {
    private val namedBranches: Map<BranchName, BranchInfo> = branches
        .filter { it.isNamed }
        .associateBy { it.name!! }

    val unnamed: List<BranchInfo> = branches.filter { it.isUnnamed }

    operator fun get(name: BranchName): BranchInfo? = namedBranches[name]
    operator fun get(name: String): BranchInfo? = namedBranches[BranchName(name)]

    fun all(): Collection<BranchInfo> = namedBranches.values + unnamed
    fun size() = namedBranches.size + unnamed.size
}

data class GitMiningResult(
    val commits: Commits,
    val branches: BranchInfos = BranchInfos(emptyList())
)