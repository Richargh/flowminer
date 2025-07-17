package de.richargh.teamcharta.importer.jira.app

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import de.richargh.teamcharta.importer.jira.app.api.JiraConnection
import de.richargh.teamcharta.importer.jira.app.api.JiraIssue
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.OffsetDateTime
import java.time.ZoneOffset

class JiraIssueService {

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()
    private val responseAdapter = moshi.adapter(JiraSearchResponse::class.java)

    fun fetchIssues(
        projectKey: String,
        jiraConnection: JiraConnection
    ): List<JiraIssue> {
        val url = buildQuery(projectKey, jiraConnection)
        val request = buildRequest(url, jiraConnection)
        val responseBody = executeRequest(request)
        return parseIssues(responseBody)
    }

    private fun buildQuery(projectKey: String, jira: JiraConnection): HttpUrl {
        val urlBuilder = "${jira.baseUrl}/rest/api/3/search/jql".toHttpUrl().newBuilder()
        urlBuilder.addQueryParameter("jql", "project=$projectKey ORDER BY created DESC")
        urlBuilder.addQueryParameter("expand", "changelog")
        urlBuilder.addQueryParameter("maxResults", "1000")
        return urlBuilder.build()
    }

    private fun buildRequest(url: HttpUrl, jira: JiraConnection): Request {
        val credentials = Credentials.basic(jira.username, jira.token)
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
        val searchResponse = responseAdapter.fromJson(responseBody)
        val now = System.currentTimeMillis()
        return searchResponse?.issues?.map { extractIssueInfo(it, now) } ?: emptyList()
    }

    private fun extractIssueInfo(issue: JiraApiIssue, now: Long): JiraIssue {
        val key = issue.key ?: ""
        val name = issue.fields?.summary
        val type = issue.fields?.issuetype?.name
        val state = issue.fields?.status?.name
        val changelog = issue.changelog
        var started: OffsetDateTime? = null
        var finished: OffsetDateTime? = null
        var stateDuration: Long? = null
        if (changelog != null && changelog.histories != null) {
            var lastStateChange: Long? = null
            for (history in changelog.histories) {
                for (item in history.items ?: emptyList()) {
                    if (item.field == "status") {
                        val toString = item.toStringValue
                        val created = history.created
                        val createdOffset = try {
                            created?.let {
                                OffsetDateTime.parse(it).withOffsetSameInstant(ZoneOffset.UTC)
                            }
                        } catch (e: Exception) {
                            null
                        }
                        val createdMillis = createdOffset?.toInstant()?.toEpochMilli()
                        if (toString == "In Progress" && started == null) {
                            started = createdOffset
                        }
                        if ((toString == "Done" || toString == "Closed") && finished == null) {
                            finished = createdOffset
                        }
                        if (toString == state) {
                            lastStateChange = createdMillis
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

@JsonClass(generateAdapter = true)
data class JiraSearchResponse(
    val issues: List<JiraApiIssue>?
)

@JsonClass(generateAdapter = true)
data class JiraApiIssue(
    val key: String?,
    val fields: JiraApiFields?,
    val changelog: JiraApiChangelog?
)

@JsonClass(generateAdapter = true)
data class JiraApiFields(
    val summary: String?,
    val issuetype: JiraApiIssueType?,
    val status: JiraApiStatus?
)

@JsonClass(generateAdapter = true)
data class JiraApiIssueType(
    val name: String?
)

@JsonClass(generateAdapter = true)
data class JiraApiStatus(
    val name: String?
)

@JsonClass(generateAdapter = true)
data class JiraApiChangelog(
    val histories: List<JiraApiHistory>?
)

@JsonClass(generateAdapter = true)
data class JiraApiHistory(
    val created: String?,
    val items: List<JiraApiHistoryItem>?
)

@JsonClass(generateAdapter = true)
data class JiraApiHistoryItem(
    val field: String?,
    @Json(name = "toString") val toStringValue: String?
)