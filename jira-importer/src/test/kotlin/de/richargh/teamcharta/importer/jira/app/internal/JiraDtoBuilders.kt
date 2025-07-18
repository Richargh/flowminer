package de.richargh.teamcharta.importer.jira.app.internal

fun jiraSearchResponseDto(block: JiraSearchResponseDtoBuilder.() -> Unit): JiraSearchResponseDto =
    JiraSearchResponseDtoBuilder().apply(block).build()

class JiraSearchResponseDtoBuilder {
    private var issues: List<JiraApiIssueDto> = emptyList()

    fun issues(vararg issues: JiraApiIssueDto) = apply { this.issues = issues.toList() }
    fun issues(issues: List<JiraApiIssueDto>) = apply { this.issues = issues }
    fun build() = JiraSearchResponseDto(issues)
}

fun jiraApiIssueDto(block: JiraApiIssueDtoBuilder.() -> Unit): JiraApiIssueDto =
    JiraApiIssueDtoBuilder().apply(block).build()

class JiraApiIssueDtoBuilder {
    private var key: String? = null
    private var fields: JiraApiFieldsDto? = null
    private var changelog: JiraApiChangelogDto? = null

    fun key(key: String?) = apply { this.key = key }
    fun fields(fields: JiraApiFieldsDto?) = apply { this.fields = fields }
    fun changelog(changelog: JiraApiChangelogDto?) = apply { this.changelog = changelog }
    fun build() = JiraApiIssueDto(key, fields, changelog)
}

fun jiraApiFieldsDto(block: JiraApiFieldsDtoBuilder.() -> Unit): JiraApiFieldsDto =
    JiraApiFieldsDtoBuilder().apply(block).build()

class JiraApiFieldsDtoBuilder {
    private var summary: String? = null
    private var issuetype: JiraApiIssueTypeDto? = null
    private var status: JiraApiStatusDto? = null

    fun summary(summary: String?) = apply { this.summary = summary }
    fun issuetype(issuetype: JiraApiIssueTypeDto?) = apply { this.issuetype = issuetype }
    fun status(status: JiraApiStatusDto?) = apply { this.status = status }
    fun build() = JiraApiFieldsDto(summary, issuetype, status)
}

fun jiraApiIssueTypeDto(block: JiraApiIssueTypeDtoBuilder.() -> Unit): JiraApiIssueTypeDto =
    JiraApiIssueTypeDtoBuilder().apply(block).build()

class JiraApiIssueTypeDtoBuilder {
    private var name: String? = null

    fun name(name: String?) = apply { this.name = name }
    fun build() = JiraApiIssueTypeDto(name)
}

fun jiraApiStatusDto(block: JiraApiStatusDtoBuilder.() -> Unit): JiraApiStatusDto =
    JiraApiStatusDtoBuilder().apply(block).build()

class JiraApiStatusDtoBuilder {
    private var name: String? = null

    fun name(name: String?) = apply { this.name = name }
    fun build() = JiraApiStatusDto(name)
}

fun jiraApiChangelogDto(block: JiraApiChangelogDtoBuilder.() -> Unit): JiraApiChangelogDto =
    JiraApiChangelogDtoBuilder().apply(block).build()

class JiraApiChangelogDtoBuilder {
    private var histories: List<JiraApiHistoryDto> = emptyList()

    fun histories(vararg histories: JiraApiHistoryDto) = apply { this.histories = histories.toList() }
    fun histories(histories: List<JiraApiHistoryDto>) = apply { this.histories = histories }
    fun build() = JiraApiChangelogDto(histories)
}

fun jiraApiHistoryDto(block: JiraApiHistoryDtoBuilder.() -> Unit): JiraApiHistoryDto =
    JiraApiHistoryDtoBuilder().apply(block).build()

class JiraApiHistoryDtoBuilder {
    private var created: String? = null
    private var items: List<JiraApiHistoryItemDto> = emptyList()

    fun created(created: String?) = apply { this.created = created }
    fun items(vararg items: JiraApiHistoryItemDto) = apply { this.items = items.toList() }
    fun items(items: List<JiraApiHistoryItemDto>) = apply { this.items = items }
    fun build() = JiraApiHistoryDto(created, items)
}

fun jiraApiHistoryItemDto(block: JiraApiHistoryItemDtoBuilder.() -> Unit): JiraApiHistoryItemDto =
    JiraApiHistoryItemDtoBuilder().apply(block).build()

class JiraApiHistoryItemDtoBuilder {
    private var field: String? = null
    private var fromStringValue: String? = null
    private var toStringValue: String? = null

    fun field(field: String?) = apply { this.field = field }
    fun fromStringValue(from: String?) = apply { this.fromStringValue = from }
    fun toStringValue(to: String?) = apply { this.toStringValue = to }
    fun build() = JiraApiHistoryItemDto(field, fromStringValue, toStringValue)
} 