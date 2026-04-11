package de.richargh.flowminer.importer.jira.app

import de.richargh.flowminer.importer.jira.app.api.JiraCredentials
import de.richargh.flowminer.importer.jira.app.api.WorkItem
import de.richargh.flowminer.importer.jira.app.dto.JiraApiIssueDto
import de.richargh.flowminer.importer.jira.app.dto.JiraCurrentUserDto
import de.richargh.flowminer.importer.jira.app.dto.JiraServerInfoDto
import de.richargh.flowminer.importer.jira.app.dto.JiraProjectDto
import de.richargh.flowminer.importer.jira.app.dto.toWorkItem
import de.richargh.flowminer.importer.jira.app.internal.JiraApiClient
import de.richargh.flowminer.importer.jira.app.internal.createJiraHttpClient
import io.ktor.client.HttpClient

class JiraImporter(
    credentials: JiraCredentials,
    httpClient: HttpClient = createJiraHttpClient()
) {
    private val apiClient = JiraApiClient(credentials, httpClient)

    suspend fun import(
        projectKey: String,
        pageSize: Int = 100,
        onProgress: (fetched: Int, total: Int) -> Unit = { _, _ -> }
    ): List<WorkItem> {
        val allIssues = mutableListOf<JiraApiIssueDto>()
        do {
            val page = apiClient.fetchIssuePage(projectKey, startAt = allIssues.size, pageSize = pageSize)
            allIssues.addAll(page.issues)
            onProgress(allIssues.size, page.total)
        } while (allIssues.size < page.total && page.issues.isNotEmpty())
        return allIssues.map { it.toWorkItem() }
    }

    suspend fun resolvedApiVersion(): String = apiClient.resolvedApiVersion()

    suspend fun serverInfo(): JiraServerInfoDto = apiClient.fetchServerInfo()

    suspend fun currentUser(): JiraCurrentUserDto = apiClient.fetchCurrentUser()

    suspend fun accessibleProjects(): List<JiraProjectDto> = apiClient.fetchProjects()

    fun close() {
        apiClient.close()
    }
}
