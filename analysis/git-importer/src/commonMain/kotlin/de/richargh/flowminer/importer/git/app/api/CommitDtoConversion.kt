package de.richargh.flowminer.importer.git.app.api

import de.richargh.flowminer.model.*
import kotlin.time.Instant

fun Commit.toDto(): SerializableCommitDto = SerializableCommitDto(
    hash = hash.rawValue,
    authorName = author.name,
    authorEmail = author.email,
    date = date.toString(),
    message = message,
    parents = parents.map { it.rawValue },
    branchId = branchId.toDto(),
    workKeys = workKeys.map { workKey ->
        when (workKey) {
            is WorkKey.Known -> workKey.key
            WorkKey.Unknown -> null
        }
    },
    commitType = commitType.name,
    fileChanges = fileChanges.map { it.toDto() },
    isOnCurrentBranch = isOnCurrentBranch,
    isMerge = isMerge
)

fun BranchId.toDto(): SerializableBranchIdDto = when (this) {
    is NamedBranchId.Certain -> SerializableBranchIdDto(
        type = "certain",
        name = name.value,
        tipCommit = null
    )

    is NamedBranchId.Inferred -> SerializableBranchIdDto(
        type = "inferred",
        name = name.value,
        tipCommit = null
    )

    is NamelessBranchId -> SerializableBranchIdDto(
        type = "nameless",
        name = null,
        tipCommit = tipCommit.rawValue
    )
}

fun FileChange.toDto(): SerializableFileChangeDto =
    SerializableFileChangeDto(
        path = path,
        additions = additions,
        deletions = deletions,
        isRename = isRename,
        oldPath = oldPath
    )

fun CommitDto.toCommit(): Commit =
    Commit(
        hash = CommitHash(hash),
        author = Author(authorName, authorEmail),
        date = Instant.parse(date),
        message = message,
        parents = parents.map { CommitHash(it) },
        refs = emptyList(),  // refs are not stored in DTO
        fileChanges = fileChanges.map { it.toFileChange() },
        trailers = emptyList(),  // trailers are not stored in DTO
        coAuthors = emptySet(),  // coAuthors are not stored in DTO
        commitType = CommitType.valueOf(commitType),
        workKeys = workKeys.map { key ->
            if (key == null) WorkKey.Unknown else WorkKey.Known(
                key
            )
        },
        branchId = branchId.toBranchId(),
        isOnCurrentBranch = isOnCurrentBranch
    )

fun BranchIdDto.toBranchId(): BranchId = when (type) {
    "certain" -> NamedBranchId.Certain(
        BranchName(
            name!!
        )
    )

    "inferred" -> NamedBranchId.Inferred(
        BranchName(name!!)
    )

    "nameless" -> NamelessBranchId(
        CommitHash(
            tipCommit!!
        )
    )

    else -> throw IllegalArgumentException("Unknown branch type: $type")
}

fun FileChangeDto.toFileChange(): FileChange =
    FileChange(
        path = path,
        additions = additions,
        deletions = deletions,
        isRename = isRename,
        oldPath = oldPath
    )
