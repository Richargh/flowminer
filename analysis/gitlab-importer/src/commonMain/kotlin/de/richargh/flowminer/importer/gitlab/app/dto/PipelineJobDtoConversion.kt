package de.richargh.flowminer.importer.gitlab.app.dto

import de.richargh.flowminer.importer.gitlab.app.api.PipelineJob
import de.richargh.flowminer.model.SerializablePipelineJobDto

fun PipelineJob.toDto(): SerializablePipelineJobDto = SerializablePipelineJobDto(
    pipelineId = pipelineId.value,
    pipelineRef = pipelineRef,
    pipelineStatus = pipelineStatus.name,
    pipelineCreatedAt = pipelineCreatedAt.toString(),
    jobId = jobId.value,
    jobName = jobName.value,
    jobStage = jobStage.value,
    jobStatus = jobStatus.name,
    jobStartedAt = jobStartedAt?.toString(),
    jobFinishedAt = jobFinishedAt?.toString(),
    jobDurationSeconds = jobDurationSeconds,
    jobAllowFailure = jobAllowFailure
)
