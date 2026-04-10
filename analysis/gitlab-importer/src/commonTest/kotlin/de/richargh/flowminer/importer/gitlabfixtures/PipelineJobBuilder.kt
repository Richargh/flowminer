package de.richargh.flowminer.importer.gitlabfixtures

import de.richargh.flowminer.importer.gitlab.app.api.JobId
import de.richargh.flowminer.importer.gitlab.app.api.JobName
import de.richargh.flowminer.importer.gitlab.app.api.JobStatus
import de.richargh.flowminer.importer.gitlab.app.api.PipelineId
import de.richargh.flowminer.importer.gitlab.app.api.PipelineJob
import de.richargh.flowminer.importer.gitlab.app.api.PipelineStatus
import de.richargh.flowminer.importer.gitlab.app.api.StageName
import kotlin.time.Instant

fun aPipelineJob(block: PipelineJobBuilder.() -> Unit = {}): PipelineJob =
    PipelineJobBuilder().apply(block).build()

class PipelineJobBuilder {
    var pipelineId: Int = 1
    var pipelineRef: String = "main"
    var pipelineStatus: PipelineStatus = PipelineStatus.success
    var pipelineCreatedAt: Instant = Instant.parse("2024-01-01T10:00:00Z")
    var jobId: Int = 42
    var jobName: String = "build"
    var jobStage: String = "build"
    var jobStatus: JobStatus = JobStatus.success
    var jobStartedAt: Instant? = Instant.parse("2024-01-01T10:01:00Z")
    var jobFinishedAt: Instant? = Instant.parse("2024-01-01T10:05:00Z")
    var jobDurationSeconds: Double? = 240.0
    var jobAllowFailure: Boolean = false

    fun build(): PipelineJob = PipelineJob(
        pipelineId = PipelineId(pipelineId),
        pipelineRef = pipelineRef,
        pipelineStatus = pipelineStatus,
        pipelineCreatedAt = pipelineCreatedAt,
        jobId = JobId(jobId),
        jobName = JobName(jobName),
        jobStage = StageName(jobStage),
        jobStatus = jobStatus,
        jobStartedAt = jobStartedAt,
        jobFinishedAt = jobFinishedAt,
        jobDurationSeconds = jobDurationSeconds,
        jobAllowFailure = jobAllowFailure
    )
}
