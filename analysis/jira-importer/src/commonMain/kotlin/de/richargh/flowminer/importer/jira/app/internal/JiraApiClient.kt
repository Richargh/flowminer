package de.richargh.flowminer.importer.jira.app.internal

import de.richargh.flowminer.importer.jira.app.api.JiraAuth
import de.richargh.flowminer.importer.jira.app.api.JiraConnectionException
import de.richargh.flowminer.importer.jira.app.api.JiraVersionDetectionException
import de.richargh.flowminer.importer.jira.app.api.JiraCredentials
import de.richargh.flowminer.importer.jira.app.api.JiraForbiddenException
import de.richargh.flowminer.importer.jira.app.api.JiraNotFoundException
import de.richargh.flowminer.importer.jira.app.api.JiraRateLimitException
import de.richargh.flowminer.importer.jira.app.api.JiraUnauthorizedException
import de.richargh.flowminer.importer.jira.app.dto.JiraCurrentUserDto
import de.richargh.flowminer.importer.jira.app.dto.JiraServerInfoDto
import de.richargh.flowminer.importer.jira.app.dto.JiraProjectDto
import de.richargh.flowminer.importer.jira.app.dto.JiraSearchResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.encodeURLQueryComponent
import io.ktor.util.encodeBase64

private fun HttpResponse.checkStatus(projectKey: String) {
    when (status) {
        HttpStatusCode.Unauthorized -> throw JiraUnauthorizedException()
        HttpStatusCode.Forbidden -> throw JiraForbiddenException(projectKey)
        HttpStatusCode.NotFound -> throw JiraNotFoundException(projectKey)
        HttpStatusCode.TooManyRequests -> throw JiraRateLimitException()
        else -> if (!status.isSuccess()) throw Exception("Unexpected Jira API response: $status")
    }
}

private fun HttpResponse.checkGeneralStatus() {
    when (status) {
        HttpStatusCode.Unauthorized -> throw JiraUnauthorizedException()
        HttpStatusCode.TooManyRequests -> throw JiraRateLimitException()
        else -> if (!status.isSuccess()) throw Exception("Unexpected Jira API response: $status")
    }
}

class JiraApiClient(
    private val credentials: JiraCredentials,
    private val httpClient: HttpClient
) {
    private var resolvedApiVersion: String? = null

    suspend fun resolvedApiVersion(): String = apiVersion()

    private suspend fun apiVersion(): String {
        if (resolvedApiVersion == null) {
            resolvedApiVersion = if (credentials.apiVersion == "auto") detectApiVersion() else credentials.apiVersion
        }
        return resolvedApiVersion!!
    }

    private suspend fun detectApiVersion(): String {
        for (version in listOf("3", "2")) {
            try {
                val response = httpClient.get("${credentials.baseUrl}/rest/api/$version/serverInfo") {
                    header("Accept", "application/json")
                }
                if (response.status.isSuccess()) return version
            } catch (_: Exception) { }
        }
        throw JiraVersionDetectionException(credentials.baseUrl)
    }

    suspend fun fetchIssuePage(projectKey: String, startAt: Int, pageSize: Int): JiraSearchResponseDto {
        val url = buildUrl(projectKey, startAt, pageSize)
        val response = try {
            httpClient.get(url) {
                header("Authorization", credentials.auth.toHeader())
                header("Accept", "application/json")
            }
        } catch (e: Exception) {
            throw JiraConnectionException(credentials.baseUrl, e)
        }
        response.checkStatus(projectKey)
        return response.body()
    }

    suspend fun fetchServerInfo(): JiraServerInfoDto {
        val url = "${credentials.baseUrl}/rest/api/${apiVersion()}/serverInfo"
        val response = try {
            httpClient.get(url) {
                header("Accept", "application/json")
            }
        } catch (e: Exception) {
            throw JiraConnectionException(credentials.baseUrl, e)
        }
        response.checkGeneralStatus()
        return response.body()
    }

    suspend fun fetchCurrentUser(): JiraCurrentUserDto {
        val url = "${credentials.baseUrl}/rest/api/${apiVersion()}/myself"
        val response = try {
            httpClient.get(url) {
                header("Authorization", credentials.auth.toHeader())
                header("Accept", "application/json")
            }
        } catch (e: Exception) {
            throw JiraConnectionException(credentials.baseUrl, e)
        }
        response.checkGeneralStatus()
        return response.body()
    }

    suspend fun fetchProjects(): List<JiraProjectDto> {
        val url = "${credentials.baseUrl}/rest/api/${apiVersion()}/project"
        val response = try {
            httpClient.get(url) {
                header("Authorization", credentials.auth.toHeader())
                header("Accept", "application/json")
            }
        } catch (e: Exception) {
            throw JiraConnectionException(credentials.baseUrl, e)
        }
        response.checkGeneralStatus()
        return response.body()
    }

    fun close() {
        httpClient.close()
    }

    private suspend fun buildUrl(projectKey: String, startAt: Int, pageSize: Int): String {
        val version = apiVersion()
        val jql = "project=$projectKey ORDER BY created DESC"
        val searchPath = if (version == "2") "search" else "search/jql"
        return "${credentials.baseUrl}/rest/api/$version/$searchPath" +
            "?jql=${jql.encodeURLQueryComponent()}" +
            "&expand=changelog" +
            "&startAt=$startAt" +
            "&maxResults=$pageSize"
    }
}

private fun JiraAuth.toHeader(): String = when (this) {
    is JiraAuth.Basic -> "Basic ${"$username:$token".encodeToByteArray().encodeBase64()}"
    is JiraAuth.Pat   -> "Bearer $token"
}
