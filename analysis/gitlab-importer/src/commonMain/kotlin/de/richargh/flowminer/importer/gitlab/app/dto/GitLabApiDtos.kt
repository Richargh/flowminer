package de.richargh.flowminer.importer.gitlab.app.dto

import kotlinx.serialization.Serializable

@Serializable
data class GitLabPipelineDto(
    val id: Int,
    val ref: String,
    val status: String,
    val created_at: String
)

@Serializable
data class GitLabJobDto(
    val id: Int,
    val name: String,
    val stage: String,
    val status: String,
    val started_at: String? = null,
    val finished_at: String? = null,
    val duration: Double? = null,
    val allow_failure: Boolean = false
)
