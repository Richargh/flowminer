package de.richargh.flowminer.shared.dto

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class AuthorStatsDto(
    val name: String,
    val commitCount: Int,
    val linesAdded: Int,
    val linesDeleted: Int,
    val avgCommitSize: Double
)
