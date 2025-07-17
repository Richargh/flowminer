package de.richargh.teamcharta.importer.jira.app.api

data class JiraIssue(
    val key: String,
    val name: String?,
    val type: String?,
    val state: String?,
    val started: String?,
    val finished: String?,
    val stateDuration: Long?
)