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
    val trailers: List<Pair<String, String>> = emptyList(),
    val coAuthors: Set<Author> = emptySet(),
    val commitTypes: List<CommitType> = emptyList()
)

data class GitMiningResult(
    val commits: List<Commit>
)
