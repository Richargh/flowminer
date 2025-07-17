package de.richargh.teamcharta.importer.jira.app

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.richargh.teamcharta.importer.jira.app.api.JiraIssue
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.OffsetDateTime


class JiraIssueService {

    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()

    fun fetchIssues(
        username: String,
        token: String,
        baseUrl: String,
        projectKey: String
    ): List<JiraIssue> {
        val url = buildQuery(baseUrl, projectKey)
        val request = buildRequest(url, username, token)
        val responseBody = executeRequest(request)
        return parseIssues(responseBody)
    }

    private fun buildQuery(baseUrl: String, projectKey: String): HttpUrl {
        val urlBuilder = "$baseUrl/rest/api/3/search/jql".toHttpUrl().newBuilder()
        urlBuilder.addQueryParameter("jql", "project=$projectKey ORDER BY created DESC")
        urlBuilder.addQueryParameter("expand", "changelog")
        urlBuilder.addQueryParameter("maxResults", "1000")
        return urlBuilder.build()
    }

    private fun buildRequest(url: HttpUrl, username: String, token: String): Request {
        val credentials = Credentials.basic(username, token)
        return Request.Builder()
            .url(url)
            .header("Authorization", credentials)
            .header("Accept", "application/json")
            .build()
    }

    private fun executeRequest(request: Request): String {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Unexpected code $response")
            }
            return response.body?.string() ?: throw RuntimeException("Empty response body")
        }
    }

    private fun parseIssues(responseBody: String): List<JiraIssue> {
        val root: JsonNode = mapper.readTree(responseBody)
        val issues: JsonNode? = root.get("issues")
        val now = System.currentTimeMillis()
        if (issues == null || !issues.isArray) return emptyList()
        return issues.elements().asSequence().map { issue ->
            extractIssueInfo(issue, now)
        }.toList()
    }

    private fun extractIssueInfo(issue: JsonNode, now: Long): JiraIssue {
        val key = issue.get("key")?.asText() ?: ""
        val fields = issue.get("fields")
        val name = fields?.get("summary")?.asText()
        val type = fields?.get("issuetype")?.get("name")?.asText()
        val state = fields?.get("status")?.get("name")?.asText()
        val changelog = issue.get("changelog")
        var started: String? = null
        var finished: String? = null
        var stateDuration: Long? = null
        if (changelog != null && changelog.has("histories")) {
            val histories = changelog.get("histories")
            var lastStateChange: Long? = null
            if (histories != null && histories.isArray) {
                val historyElements = histories.elements()
                while (historyElements.hasNext()) {
                    val history = historyElements.next()
                    val items = history.get("items")
                    if (items != null && items.isArray) {
                        val itemElements = items.elements()
                        while (itemElements.hasNext()) {
                            val item = itemElements.next()
                            if (item.get("field")?.asText() == "status") {
                                val toString = item.get("toString")?.asText()
                                val created = history.get("created")?.asText()
                                val createdMillis = try {
                                    created?.let {
                                        OffsetDateTime.parse(it).toInstant().toEpochMilli()
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                                if (toString == "In Progress" && started == null) {
                                    started = created
                                }
                                if ((toString == "Done" || toString == "Closed") && finished == null) {
                                    finished = created
                                }
                                if (toString == state) {
                                    lastStateChange = createdMillis
                                }
                            }
                        }
                    }
                }
            }
            if (lastStateChange != null) {
                stateDuration = (now - lastStateChange) / 1000 // seconds
            }
        }
        return JiraIssue(key, name, type, state, started, finished, stateDuration)
    }
}