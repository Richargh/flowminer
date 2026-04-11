package de.richargh.flowminer.importer.jira.app.dto

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class JiraApiDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `should deserialize a Jira search response JSON string`() {
        val jsonString = """
            {
              "issues": [
                {
                  "key": "PROJ-1",
                  "fields": {
                    "summary": "Test Bug",
                    "issuetype": { "name": "Bug" },
                    "status": { "name": "In Progress" }
                  },
                  "changelog": {
                    "histories": [
                      {
                        "created": "2024-06-01T10:00:00.000+0000",
                        "items": [
                          {
                            "field": "status",
                            "fromString": "To Do",
                            "toString": "In Progress"
                          }
                        ]
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<JiraSearchResponseDto>(jsonString)

        response.issues.size shouldBe 1
        val issue = response.issues[0]
        issue.key shouldBe "PROJ-1"
        issue.fields?.summary shouldBe "Test Bug"
        issue.fields?.issuetype?.name shouldBe "Bug"
        issue.fields?.status?.name shouldBe "In Progress"
        val history = issue.changelog?.histories?.get(0)
        history?.created shouldBe "2024-06-01T10:00:00.000+0000"
        val item = history?.items?.get(0)
        item?.field shouldBe "status"
        item?.fromString shouldBe "To Do"
        item?.toStringValue shouldBe "In Progress"
    }

    @Test
    fun `should serialize and deserialize a JiraSearchResponseDto round-trip`() {
        val original = JiraSearchResponseDto(
            issues = listOf(
                JiraApiIssueDto(
                    key = "ROUND-1",
                    fields = JiraApiFieldsDto(summary = "Round trip"),
                    changelog = null
                )
            )
        )

        val encoded = json.encodeToString(JiraSearchResponseDto.serializer(), original)
        val decoded = json.decodeFromString<JiraSearchResponseDto>(encoded)

        decoded.issues.size shouldBe 1
        decoded.issues[0].key shouldBe "ROUND-1"
        decoded.issues[0].fields?.summary shouldBe "Round trip"
    }
}
