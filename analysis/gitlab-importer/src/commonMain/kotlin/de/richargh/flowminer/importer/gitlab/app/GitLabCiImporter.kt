package de.richargh.flowminer.importer.gitlab.app

import de.richargh.flowminer.importer.gitlab.app.api.GitLabCredentials
import de.richargh.flowminer.importer.gitlab.app.api.JobId
import de.richargh.flowminer.importer.gitlab.app.api.JobName
import de.richargh.flowminer.importer.gitlab.app.api.JobStatus
import de.richargh.flowminer.importer.gitlab.app.api.PipelineId
import de.richargh.flowminer.importer.gitlab.app.api.PipelineJob
import de.richargh.flowminer.importer.gitlab.app.api.PipelineStatus
import de.richargh.flowminer.importer.gitlab.app.api.StageName
import de.richargh.flowminer.importer.gitlab.app.internal.GitLabApiClient
import de.richargh.flowminer.importer.gitlab.app.dto.GitLabJobDto
import de.richargh.flowminer.importer.gitlab.app.dto.GitLabPipelineDto
import de.richargh.flowminer.importer.gitlab.app.internal.createGitLabHttpClient
import io.ktor.client.HttpClient
import kotlin.time.Instant

class GitLabCiImporter(
    private val credentials: GitLabCredentials,
    private val httpClient: HttpClient = createGitLabHttpClient()
) {
    private val apiClient: GitLabApiClient = GitLabApiClient(credentials, httpClient)

    suspend fun import(projectId: String, count: Int = 10, log: (String) -> Unit = {}): List<PipelineJob> {
        log("Fetching $count most recent pipelines for $projectId...")
        val pipelines = apiClient.fetchRecentPipelines(projectId, count)
        return pipelines.mapIndexed { index, pipeline ->
            log("  [${index + 1}/${pipelines.size}] Pipeline #${pipeline.id} (${pipeline.ref}): fetching jobs...")
            val jobs = apiClient.fetchJobsForPipeline(projectId, pipeline.id)
            log("    → ${jobs.size} jobs")
            jobs.map { job -> toPipelineJob(pipeline, job) }
        }.flatten()
    }

    fun close() {
        apiClient.close()
    }

    private fun toPipelineJob(pipeline: GitLabPipelineDto, job: GitLabJobDto): PipelineJob {
        val pipelineStatus = runCatching { PipelineStatus.valueOf(pipeline.status) }
            .getOrDefault(PipelineStatus.created)
        val jobStatus = runCatching { JobStatus.valueOf(job.status) }
            .getOrDefault(JobStatus.created)

        return PipelineJob(
            pipelineId = PipelineId(pipeline.id),
            pipelineRef = pipeline.ref,
            pipelineStatus = pipelineStatus,
            pipelineCreatedAt = Instant.parse(pipeline.created_at),
            jobId = JobId(job.id),
            jobName = JobName(job.name),
            jobStage = StageName(job.stage),
            jobStatus = jobStatus,
            jobStartedAt = job.started_at?.let { Instant.parse(it) },
            jobFinishedAt = job.finished_at?.let { Instant.parse(it) },
            jobDurationSeconds = job.duration,
            jobAllowFailure = job.allow_failure
        )
    }
}
