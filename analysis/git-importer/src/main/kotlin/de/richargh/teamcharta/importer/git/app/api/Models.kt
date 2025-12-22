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

sealed interface WorkKey {
    data class Known(val key: String) : WorkKey {
        override fun toString() = key
    }
    data object Unknown : WorkKey {
        override fun toString() = "(no-workitem)"
    }
}
data class CommitHash(val rawValue: String) {
    override fun toString() = rawValue
}
data class BranchName(val value: String) {
    override fun toString() = value
}

sealed interface BranchId {
    val name: BranchName?
}

sealed interface NamedBranchId: BranchId {
    override val name: BranchName

    data class Certain(override val name: BranchName) : NamedBranchId {
        override fun toString(): String = name.toString()
    }

    data class Inferred(override val name: BranchName) : NamedBranchId {
        override fun toString(): String = "~$name"
    }
}

data class NamelessBranchId(val tipCommit: CommitHash): BranchId {
    override val name: BranchName? = null
    override fun toString(): String = "NamelessBranch($tipCommit)"
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
    val branchId: BranchId,
    val isOnCurrentBranch: Boolean = false
) {
    val isNotOnCurrentBranch = !isOnCurrentBranch
    val isMerge = parents.size >= 2
    val isNotMergeCommit = !isMerge
}

