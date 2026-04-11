package de.richargh.flowminer.importer.jirafixtures

import de.richargh.flowminer.importer.jira.app.dto.JiraApiChangelogDto
import de.richargh.flowminer.importer.jira.app.dto.JiraApiFieldsDto
import de.richargh.flowminer.importer.jira.app.dto.JiraApiHistoryDto
import de.richargh.flowminer.importer.jira.app.dto.JiraApiHistoryItemDto
import de.richargh.flowminer.importer.jira.app.dto.JiraApiIssueDto
import de.richargh.flowminer.importer.jira.app.dto.JiraApiIssueTypeDto
import de.richargh.flowminer.importer.jira.app.dto.JiraApiStatusDto
import de.richargh.flowminer.importer.jira.app.dto.JiraSearchResponseDto

fun aJiraSearchResponse(block: JiraSearchResponseBuilder.() -> Unit = {}): JiraSearchResponseDto =
    JiraSearchResponseBuilder().apply(block).build()

class JiraSearchResponseBuilder {
    var issues: List<JiraApiIssueDto> = emptyList()

    fun issues(vararg issues: JiraApiIssueDto) = apply { this.issues = issues.toList() }
    fun build() = JiraSearchResponseDto(total = issues.size, issues = issues)
}

fun aJiraIssue(block: JiraIssueBuilder.() -> Unit = {}): JiraApiIssueDto =
    JiraIssueBuilder().apply(block).build()

class JiraIssueBuilder {
    var key: String? = "TEST-1"
    var fields: JiraApiFieldsDto? = null
    var changelog: JiraApiChangelogDto? = null

    fun build() = JiraApiIssueDto(key, fields, changelog)
}

fun aJiraFields(block: JiraFieldsBuilder.() -> Unit = {}): JiraApiFieldsDto =
    JiraFieldsBuilder().apply(block).build()

class JiraFieldsBuilder {
    var summary: String? = "Test Issue"
    var issuetype: JiraApiIssueTypeDto? = null
    var status: JiraApiStatusDto? = null

    fun build() = JiraApiFieldsDto(summary, issuetype, status)
}

fun aJiraIssueType(block: JiraIssueTypeBuilder.() -> Unit = {}): JiraApiIssueTypeDto =
    JiraIssueTypeBuilder().apply(block).build()

class JiraIssueTypeBuilder {
    var name: String? = "Story"

    fun build() = JiraApiIssueTypeDto(name)
}

fun aJiraStatus(block: JiraStatusBuilder.() -> Unit = {}): JiraApiStatusDto =
    JiraStatusBuilder().apply(block).build()

class JiraStatusBuilder {
    var name: String? = "To Do"

    fun build() = JiraApiStatusDto(name)
}

fun aJiraChangelog(block: JiraChangelogBuilder.() -> Unit = {}): JiraApiChangelogDto =
    JiraChangelogBuilder().apply(block).build()

class JiraChangelogBuilder {
    var histories: List<JiraApiHistoryDto> = emptyList()

    fun histories(vararg histories: JiraApiHistoryDto) = apply { this.histories = histories.toList() }
    fun build() = JiraApiChangelogDto(histories)
}

fun aJiraHistory(block: JiraHistoryBuilder.() -> Unit = {}): JiraApiHistoryDto =
    JiraHistoryBuilder().apply(block).build()

class JiraHistoryBuilder {
    var created: String? = "2024-06-01T10:00:00.000+0000"
    var items: List<JiraApiHistoryItemDto> = emptyList()

    fun items(vararg items: JiraApiHistoryItemDto) = apply { this.items = items.toList() }
    fun build() = JiraApiHistoryDto(created, items)
}

fun aJiraHistoryItem(block: JiraHistoryItemBuilder.() -> Unit = {}): JiraApiHistoryItemDto =
    JiraHistoryItemBuilder().apply(block).build()

class JiraHistoryItemBuilder {
    var field: String? = "status"
    var fromStatus: String? = null
    var toStatus: String? = null

    fun build() = JiraApiHistoryItemDto(field = field, fromString = fromStatus, toStringValue = toStatus)
}
