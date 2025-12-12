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
    val hash: String,
    val author: Author,
    val date: ZonedDateTime,
    val message: String,
    val parents: List<String>,
    val refs: List<String>,
    val fileChanges: List<FileChange>,
    val trailers: List<Pair<String, String>>,
    val coAuthors: Set<Author>,
    val commitType: CommitType,
    val workKeys: List<WorkKey>
)

data class BranchInfo(
    val name: String,
    val firstCommitHash: String,
    val firstCommitDate: ZonedDateTime,
    val mergeCommitHash: String?,
    val mergeDate: ZonedDateTime?,
    val targetBranch: String?
) {
    companion object {
        fun unmerged(
            name: String,
            firstCommitHash: String,
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
            name: String,
            firstCommitHash: String,
            firstCommitDate: ZonedDateTime,
            mergeCommitHash: String,
            mergeDate: ZonedDateTime,
            targetBranch: String): BranchInfo {
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
    private val branchFor: Map<String, BranchInfo> = branches.associateBy { it.name }

    operator fun get(name: String): BranchInfo? = branchFor[name]

    fun all() = branchFor.values
    fun size() = branchFor.size
}

data class GitMiningResult(
    val commits: List<Commit>,
    val branches: BranchInfos = BranchInfos(emptyList())
)
