package de.richargh.teamcharta.shared.dto

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class VisualizationDataDto(
    val authors: List<AuthorStatsDto>,
    val commitTimeline: List<CommitTimelineDto>,
    val workItems: List<WorkItemDurationDto>
)
