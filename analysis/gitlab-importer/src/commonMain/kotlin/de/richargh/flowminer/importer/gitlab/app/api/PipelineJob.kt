package de.richargh.flowminer.importer.gitlab.app.api

import kotlin.time.Instant

data class PipelineId(val value: Int)
data class JobId(val value: Int)
data class JobName(val value: String)
data class StageName(val value: String)

enum class PipelineStatus {
    created, waiting_for_resource, preparing, pending, running, success, failed, canceled, skipped, manual, scheduled
}

enum class JobStatus {
    created, waiting_for_resource, preparing, pending, running, success, failed, canceled, skipped, manual, scheduled
}

data class PipelineJob(
    val pipelineId: PipelineId,
    val pipelineRef: String,
    val pipelineStatus: PipelineStatus,
    val pipelineCreatedAt: Instant,
    val jobId: JobId,
    val jobName: JobName,
    val jobStage: StageName,
    val jobStatus: JobStatus,
    val jobStartedAt: Instant?,
    val jobFinishedAt: Instant?,
    val jobDurationSeconds: Double?,
    val jobAllowFailure: Boolean
)
