package de.richargh.flowminer.importer.gitlabfixtures

import de.richargh.flowminer.importer.gitlab.app.dto.GitLabJobDto
import de.richargh.flowminer.importer.gitlab.app.dto.GitLabPipelineDto

fun aGitLabPipelineDto(block: GitLabPipelineDtoBuilder.() -> Unit = {}): GitLabPipelineDto =
    GitLabPipelineDtoBuilder().apply(block).build()

class GitLabPipelineDtoBuilder {
    var id: Int = 1
    var ref: String = "main"
    var status: String = "success"
    var created_at: String = "2024-01-01T10:00:00.000Z"

    fun build() = GitLabPipelineDto(
        id = id,
        ref = ref,
        status = status,
        created_at = created_at
    )
}

fun aGitLabJobDto(block: GitLabJobDtoBuilder.() -> Unit = {}): GitLabJobDto =
    GitLabJobDtoBuilder().apply(block).build()

class GitLabJobDtoBuilder {
    var id: Int = 10
    var name: String = "build"
    var stage: String = "build"
    var status: String = "success"
    var started_at: String? = "2024-01-01T10:01:00.000Z"
    var finished_at: String? = "2024-01-01T10:05:00.000Z"
    var duration: Double? = 240.0
    var allow_failure: Boolean = false

    fun build() = GitLabJobDto(
        id = id,
        name = name,
        stage = stage,
        status = status,
        started_at = started_at,
        finished_at = finished_at,
        duration = duration,
        allow_failure = allow_failure
    )
}
