package de.richargh.teamcharta.importer.jira.app

import de.richargh.teamcharta.importer.jira.app.api.JiraConnection
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JiraIssueServiceTest {

    private val server = MockWebServer()

    @BeforeAll
    fun beforeAll(){
        server.start()
    }

    @AfterAll
    fun afterAll(){
        server.shutdown()
    }

    @Test
    fun `fetchIssues parses issues from mocked jira server`() {
        // given
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

        val baseUrl = server.url("/").toString().removeSuffix("/")
        // when
        val service = JiraIssueService()
        val issues = service.fetchIssues(
            projectKey = "PROJ",
            JiraConnection(
                username = "user",
                token = "token",
                baseUrl = baseUrl,
            )
        )
        // then
        issues.size shouldBe 1
        val issue = issues[0]
        issue.key shouldBe "PROJ-1"
        issue.name shouldBe "Test Issue"
        issue.type shouldBe "Bug"
        issue.state shouldBe "In Progress"
    }
}