package de.richargh.flowminer.importer.jira

import com.squareup.moshi.Moshi
import de.richargh.flowminer.importer.jira.app.internal.JiraSearchResponseDto
import de.richargh.flowminer.importer.jira.app.internal.jiraApiChangelogDto
import de.richargh.flowminer.importer.jira.app.internal.jiraApiFieldsDto
import de.richargh.flowminer.importer.jira.app.internal.jiraApiHistoryDto
import de.richargh.flowminer.importer.jira.app.internal.jiraApiHistoryItemDto
import de.richargh.flowminer.importer.jira.app.internal.jiraApiIssueDto
import de.richargh.flowminer.importer.jira.app.internal.jiraApiIssueTypeDto
import de.richargh.flowminer.importer.jira.app.internal.jiraApiStatusDto
import de.richargh.flowminer.importer.jira.app.internal.jiraSearchResponseDto
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JiraImporterTest {

    private val server = MockWebServer()

    @BeforeAll
    fun beforeAll() {
        server.start()
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    fun `extracts issues from mocked jira server`(@TempDir tempDir: Path) {
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
        val outputFile = File(tempDir.toFile(), "output.json")
        // when
        JiraImporter().apply {
            username = "user"
            token = "token"
            this.baseUrl = baseUrl
            projectKey = "PROJ"
            output = outputFile.absolutePath
        }.call()
        // then
        outputFile.exists() shouldBe true
        val json = outputFile.readText()
        json shouldContain "Test Issue"
        json shouldContain "PROJ-1"
        json shouldContain "Bug"
        json shouldContain "In Progress"
    }
} 