package de.richargh.teamcharta.importer.git.app.api

import java.time.ZonedDateTime

enum class CommitType {
    FEATURE,
    FIX,
    REFACTOR,
    TEST,
    DOCS,
    ENVIRONMENT,
    UNKNOWN
}

data class WorkKey(val key: String)
data class CommitHash(val rawValue: String)
data class BranchName(val value: String) {
    override fun toString() = value
}

sealed interface BranchAssignment {
    /** Branch ref exists on this commit or was propagated via first-parent */
    data class Certain(val name: BranchName) : BranchAssignment

    /** Inferred from merge commit message (e.g., "Merge branch 'feature'") */
    data class Inferred(val name: BranchName) : BranchAssignment

    object Unknown: BranchAssignment
}

/** Certainty level of a branch's name in BranchInfo */
sealed interface NameCertainty {
    /** Branch ref exists - name is certain */
    data class Certain(val name: BranchName) : NameCertainty

    /** Inferred from merge commit message */
    data class Inferred(val name: BranchName) : NameCertainty

    /** Cannot determine name - branch is unnamed */
    object Nameless : NameCertainty
}

sealed interface Ref {
    data class LocalHead(val branch: BranchName) : Ref {
        constructor(name: String): this(BranchName(name))
    }
    data class BranchTip(val name: BranchName) : Ref {
        constructor(name: String): this(BranchName(name))
    }
    data class Tag(val name: String) : Ref
}

data class Author(
    val name: String,
    val email: String
)

data class FileChange(
    val path: String,
    val additions: Int,
    val deletions: Int,
    val isRename: Boolean = false,
    val oldPath: String? = null
)

data class Commit(
    val hash: CommitHash,
    val author: Author,
    val date: ZonedDateTime,
    val message: String,
    val parents: List<CommitHash>,
    val refs: List<Ref>,
    val fileChanges: List<FileChange>,
    val trailers: List<Pair<String, String>>,
    val coAuthors: Set<Author>,
    val commitType: CommitType,
    val workKeys: List<WorkKey>,
    val branch: BranchAssignment? = null,
    val isOnActiveBranch: Boolean = false
) {
    val isMergeCommit = parents.size >= 2
}

data class BranchInfo(
    val nameCertainty: NameCertainty,
    val firstCommitHash: CommitHash,
    val firstCommitDate: ZonedDateTime,
    val mergeCommitHash: CommitHash?,
    val mergeDate: ZonedDateTime?,
    val targetBranch: BranchName?
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
