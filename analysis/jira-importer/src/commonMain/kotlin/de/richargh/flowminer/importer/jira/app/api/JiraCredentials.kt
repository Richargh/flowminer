package de.richargh.flowminer.importer.jira.app.api

sealed class JiraAuth {
    data class Basic(val username: String, val token: String) : JiraAuth()
    data class Pat(val token: String) : JiraAuth()
}

data class JiraCredentials(
    val auth: JiraAuth,
    val baseUrl: String,
    val apiVersion: String = "auto"
)
