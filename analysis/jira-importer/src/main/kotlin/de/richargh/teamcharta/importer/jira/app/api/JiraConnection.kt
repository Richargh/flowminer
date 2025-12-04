package de.richargh.teamcharta.importer.jira.app.api

data class JiraConnection(
    val username: String,
    val token: String,
    val baseUrl: String,
)