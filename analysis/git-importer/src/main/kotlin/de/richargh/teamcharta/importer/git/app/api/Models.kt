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

sealed interface NameCertainty {
    /** Branch ref exists on this commit or was propagated via first-parent */
    data class Certain(val name: BranchName) : NameCertainty

    /** Inferred from merge commit message (e.g., "Merge branch 'feature'") */
    data class Inferred(val name: BranchName) : NameCertainty

    object Nameless: NameCertainty
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
    val branch: NameCertainty? = null,
    val isOnActiveBranch: Boolean = false
) {
    val isMergeCommit = parents.size >= 2
}

