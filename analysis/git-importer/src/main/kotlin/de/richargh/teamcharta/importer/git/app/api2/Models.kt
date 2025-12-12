package de.richargh.teamcharta.importer.git.app.api2

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

sealed interface Ref {
    data class Head(val branchName: String) : Ref
    data class Branch(val name: String) : Ref
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
    val workKeys: List<WorkKey>
)

data class BranchInfo(
    val name: Ref.Branch,
    val firstCommitHash: CommitHash,
    val firstCommitDate: ZonedDateTime,
    val mergeCommitHash: CommitHash?,
    val mergeDate: ZonedDateTime?,
    val targetBranch: Ref.Branch?
) {
    companion object {
        fun unmerged(
            name: Ref.Branch,
            firstCommitHash: CommitHash,
            firstCommitDate: ZonedDateTime): BranchInfo {
            return BranchInfo(
                name,
                firstCommitHash,
                firstCommitDate,
                null,
                null,
                null)
        }

        fun merged(
            name: Ref.Branch,
            firstCommitHash: CommitHash,
            firstCommitDate: ZonedDateTime,
            mergeCommitHash: CommitHash,
            mergeDate: ZonedDateTime,
            targetBranch: Ref.Branch): BranchInfo {
            return BranchInfo(
                name,
                firstCommitHash,
                firstCommitDate,
                mergeCommitHash,
                mergeDate,
                targetBranch)
        }
    }
}

class BranchInfos(branches: List<BranchInfo>) {
    private val branchFor: Map<String, BranchInfo> = branches.associateBy { it.name.name }

    operator fun get(name: String): BranchInfo? = branchFor[name]

    fun all() = branchFor.values
    fun size() = branchFor.size
}

data class GitMiningResult(
    val commits: List<Commit>,
    val branches: BranchInfos = BranchInfos(emptyList())
)
