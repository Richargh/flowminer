package de.richargh.flowminer.shared.dto

import de.richargh.flowminer.model.SerializablePipelineJobDto
import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class CiMiningResultDto(
    val jobs: List<SerializablePipelineJobDto>
)
