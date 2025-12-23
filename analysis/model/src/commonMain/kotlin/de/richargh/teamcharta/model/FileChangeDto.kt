package de.richargh.teamcharta.model

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
interface FileChangeDto {
    val path: String
    val additions: Int
    val deletions: Int
    val isRename: Boolean
    val oldPath: String?
}

@Serializable
data class SerializableFileChangeDto(
    override val path: String,
    override val additions: Int,
    override val deletions: Int,
    override val isRename: Boolean,
    override val oldPath: String?
) : FileChangeDto
