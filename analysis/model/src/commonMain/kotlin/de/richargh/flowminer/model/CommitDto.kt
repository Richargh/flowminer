package de.richargh.flowminer.model

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
interface CommitDto {
    val hash: String
    val authorName: String
    val authorEmail: String
    val date: String  // ISO-8601 format
    val message: String
    val parents: List<String>
    val branchId: BranchIdDto
    val workKeys: List<String?>  // null for WorkKey.Unknown
    val commitType: String
    val fileChanges: List<FileChangeDto>
    val isOnCurrentBranch: Boolean
    val isMerge: Boolean
}

@Serializable
data class SerializableCommitDto(
    override val hash: String,
    override val authorName: String,
    override val authorEmail: String,
    override val date: String,
    override val message: String,
    override val parents: List<String>,
    override val branchId: SerializableBranchIdDto,
    override val workKeys: List<String?>,
    override val commitType: String,
    override val fileChanges: List<SerializableFileChangeDto>,
    override val isOnCurrentBranch: Boolean,
    override val isMerge: Boolean
) : CommitDto
