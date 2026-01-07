package de.richargh.flowminer.model

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
interface BranchIdDto {
    val type: String        // "certain", "inferred", "nameless"
    val name: String?       // branch name (for certain/inferred)
    val tipCommit: String?  // tip commit hash (for nameless)
}

@Serializable
data class SerializableBranchIdDto(
    override val type: String,
    override val name: String?,
    override val tipCommit: String?
) : BranchIdDto
