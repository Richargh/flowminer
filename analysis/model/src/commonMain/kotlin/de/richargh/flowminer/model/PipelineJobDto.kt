package de.richargh.flowminer.model

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
interface PipelineJobDto {
    val pipelineId: Int
    val pipelineRef: String
    val pipelineStatus: String
    val pipelineCreatedAt: String
    val jobId: Int
    val jobName: String
    val jobStage: String
    val jobStatus: String
    val jobStartedAt: String?
    val jobFinishedAt: String?
    val jobDurationSeconds: Double?
    val jobAllowFailure: Boolean
}

@Serializable
data class SerializablePipelineJobDto(
    override val pipelineId: Int,
    override val pipelineRef: String,
    override val pipelineStatus: String,
    override val pipelineCreatedAt: String,
    override val jobId: Int,
    override val jobName: String,
    override val jobStage: String,
    override val jobStatus: String,
    override val jobStartedAt: String?,
    override val jobFinishedAt: String?,
    override val jobDurationSeconds: Double?,
    override val jobAllowFailure: Boolean
) : PipelineJobDto
