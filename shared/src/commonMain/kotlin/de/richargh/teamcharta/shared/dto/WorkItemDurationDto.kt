package de.richargh.teamcharta.shared.dto

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class WorkItemDurationDto(
    val key: String,
    val type: String,
    val startDate: String,
    val durationDays: Double
)
