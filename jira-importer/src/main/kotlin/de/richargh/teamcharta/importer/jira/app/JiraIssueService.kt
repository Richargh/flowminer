package de.richargh.teamcharta.importer.jira.app

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.OffsetDateTime
import java.util.Base64

class JiraIssueService {
    data class IssueInfo(
        val key: String,
        val name: String?,
        val type: String?,
        val state: String?,
        val started: String?,
        val finished: String?,
        val stateDuration: Long?
    )

    fun fetchIssues(
        username: String,
        token: String,
        baseUrl: String,
        projectKey: String
    ): List<IssueInfo> {
        val jql = "project=$projectKey ORDER BY created DESC"
        val urlString = "$baseUrl/rest/api/3/search?jql=" + URLEncoder.encode(
            jql,
            "UTF-8"
        ) + "&expand=changelog&maxResults=1000"
        val conn = URI.create(urlString).toURL().openConnection() as HttpURLConnection
        val auth = Base64.getEncoder().encodeToString("$username:$token".toByteArray())
        conn.setRequestProperty("Authorization", "Basic $auth")
        conn.setRequestProperty("Accept", "application/json")
        val response = conn.inputStream.bufferedReader().readText()
        val mapper = jacksonObjectMapper()
        val root: JsonNode = mapper.readTree(response)
        val issues: JsonNode? = root.get("issues")
        val now = System.currentTimeMillis()
        val result = mutableListOf<IssueInfo>()
        if (issues != null && issues.isArray) {
            val issueElements = issues.elements()
            while (issueElements.hasNext()) {
                val issue = issueElements.next()
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
                result.add(IssueInfo(key, name, type, state, started, finished, stateDuration))
            }
        }
        return result
    }
}