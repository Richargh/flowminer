package de.richargh.teamcharta.importer.jira.app

import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test

class JiraIssueServiceTest {
    @Test
    fun `fetchIssues parses issues from mocked jira server`() {
        val server = MockWebServer()
        val mockResponse = """
        {
          "issues": [
            {
              "key": "PROJ-1",
              "fields": {
                "summary": "Test Issue",
                "issuetype": { "name": "Bug" },
                "status": { "name": "In Progress" }
              },
              "changelog": {
                "histories": [
                  {
                    "created": "2024-06-01T10:00:00.000+0000",
                    "items": [
                      { "field": "status", "toString": "In Progress" }
                    ]
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent()
        server.enqueue(MockResponse().setBody(mockResponse).setHeader("Content-Type", "application/json"))
        server.start()
        try {
            val baseUrl = server.url("/").toString().removeSuffix("/")
            val service = JiraIssueService()
            val issues = service.fetchIssues(
                username = "user",
                token = "token",
                baseUrl = baseUrl,
                projectKey = "PROJ"
            )
            issues.size shouldBe 1
            val issue = issues[0]
            issue.key shouldBe "PROJ-1"
            issue.name shouldBe "Test Issue"
            issue.type shouldBe "Bug"
            issue.state shouldBe "In Progress"
        } finally {
            server.shutdown()
        }
    }
}