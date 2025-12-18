package de.richargh.teamcharta.shared.dto

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class CommitTimelineDto(
    val date: String,
    val cumulativeCount: Int,
    val author: String
)
