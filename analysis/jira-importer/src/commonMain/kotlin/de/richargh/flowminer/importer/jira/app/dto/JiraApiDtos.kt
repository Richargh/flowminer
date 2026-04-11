package de.richargh.flowminer.importer.jira.app.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JiraSearchResponseDto(
    val total: Int = 0,
    val startAt: Int = 0,
    val issues: List<JiraApiIssueDto> = emptyList()
)

@Serializable
data class JiraProjectDto(
    val key: String? = null,
    val name: String? = null
)

@Serializable
data class JiraCurrentUserDto(
    val displayName: String? = null,
    val emailAddress: String? = null,
    val active: Boolean? = null
)

@Serializable
data class JiraServerInfoDto(
    val serverTitle: String? = null,
    val version: String? = null,
    val deploymentType: String? = null,
    val baseUrl: String? = null
)

@Serializable
data class JiraApiIssueDto(
    val key: String? = null,
    val fields: JiraApiFieldsDto? = null,
    val changelog: JiraApiChangelogDto? = null
)

@Serializable
data class JiraApiFieldsDto(
    val summary: String? = null,
    val issuetype: JiraApiIssueTypeDto? = null,
    val status: JiraApiStatusDto? = null
)

@Serializable
data class JiraApiIssueTypeDto(
    val name: String? = null
)

@Serializable
data class JiraApiStatusDto(
    val name: String? = null
)

@Serializable
data class JiraApiChangelogDto(
    val histories: List<JiraApiHistoryDto> = emptyList()
)

@Serializable
data class JiraApiHistoryDto(
    val created: String? = null,
    val items: List<JiraApiHistoryItemDto> = emptyList()
)

@Serializable
data class JiraApiHistoryItemDto(
    val field: String? = null,
    val fromString: String? = null,
    @SerialName("toString") val toStringValue: String? = null
)
