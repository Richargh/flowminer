package de.richargh.teamcharta.importer.jira.app.internal

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JiraSearchResponseDto(
    val issues: List<JiraApiIssueDto>?
)

@JsonClass(generateAdapter = true)
data class JiraApiIssueDto(
    val key: String?,
    val fields: JiraApiFieldsDto?,
    val changelog: JiraApiChangelogDto?
)

@JsonClass(generateAdapter = true)
data class JiraApiFieldsDto(
    val summary: String?,
    val issuetype: JiraApiIssueTypeDto?,
    val status: JiraApiStatusDto?
)

@JsonClass(generateAdapter = true)
data class JiraApiIssueTypeDto(
    val name: String?
)

@JsonClass(generateAdapter = true)
data class JiraApiStatusDto(
    val name: String?
)

@JsonClass(generateAdapter = true)
data class JiraApiChangelogDto(
    val histories: List<JiraApiHistoryDto>?
)

@JsonClass(generateAdapter = true)
data class JiraApiHistoryDto(
    val created: String?,
    val items: List<JiraApiHistoryItemDto>?
)

@JsonClass(generateAdapter = true)
data class JiraApiHistoryItemDto(
    val field: String?,
    @Json(name = "fromString") val fromStringValue: String?,
    @Json(name = "toString") val toStringValue: String?
)