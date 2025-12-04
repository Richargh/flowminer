package de.richargh.teamcharta.importer.jira.app

import com.squareup.moshi.Moshi
import de.richargh.teamcharta.importer.jira.app.api.JiraConnection
import de.richargh.teamcharta.importer.jira.app.internal.JiraSearchResponseDto
import de.richargh.teamcharta.importer.jira.app.internal.jiraApiChangelogDto
import de.richargh.teamcharta.importer.jira.app.internal.jiraApiFieldsDto
import de.richargh.teamcharta.importer.jira.app.internal.jiraApiHistoryDto
import de.richargh.teamcharta.importer.jira.app.internal.jiraApiHistoryItemDto
import de.richargh.teamcharta.importer.jira.app.internal.jiraApiIssueDto
import de.richargh.teamcharta.importer.jira.app.internal.jiraApiIssueTypeDto
import de.richargh.teamcharta.importer.jira.app.internal.jiraApiStatusDto
import de.richargh.teamcharta.importer.jira.app.internal.jiraSearchResponseDto
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
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(JiraSearchResponseDto::class.java)
        val mockResponseObj = jiraSearchResponseDto {
            issues(
                jiraApiIssueDto {
                    key("PROJ-1")
                    fields(jiraApiFieldsDto {
                        summary("Test Issue")
                        issuetype(jiraApiIssueTypeDto { name("Bug") })
                        status(jiraApiStatusDto { name("In Progress") })
                    })
                    changelog(jiraApiChangelogDto {
                        histories(
                            jiraApiHistoryDto {
                                created("2024-06-01T10:00:00.000+0000")
                                items(
                                    jiraApiHistoryItemDto {
                                        field("status")
                                        fromStringValue("To Do")
                                        toStringValue("In Progress")
                                    }
                                )
                            }
                        )
                    })
                }
            )
        }
        val mockResponse = adapter.toJson(mockResponseObj)
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